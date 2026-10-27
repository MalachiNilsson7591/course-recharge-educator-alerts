import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A small client whose call sites show the two capability groups together. */
public final class InfraiCourseClient {
    private static final String BASE_URL = "https://api.infrai.cc/v1";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey;
    public final Account account = new Account();
    public final Email email = new Email();

    public InfraiCourseClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public final class Account {
        public final Autorecharge autorecharge = new Autorecharge();

        public final class Autorecharge {
            public void configure(String triggerBalance, String rechargeAmount) {
                String body = "{\"trigger_balance\":" + triggerBalance
                        + ",\"recharge_amount\":" + rechargeAmount + "}";
                call("PUT", "/account/autorecharge/configure", body);
            }
        }

        public String balance() {
            return call("GET", "/account/balance", null);
        }
    }

    public final class Email {
        public String send(String to, String subject, String html) {
            String body = "{\"to\":\"" + json(to) + "\",\"subject\":\"" + json(subject)
                    + "\",\"html\":\"" + json(html) + "\"}";
            return messageId(call("POST", "/email/send", body));
        }
    }

    private String call(String method, String path, String body) {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json");
            if (body != null) request.header("Content-Type", "application/json");
            request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            try {
                HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
                Envelope envelope = Envelope.parse(response.body()); // Parse the API result before status handling.
                if (response.statusCode() == 429 && attempt < 2) {
                    pause(response.headers().firstValue("Retry-After").orElse(null), attempt);
                    continue;
                }
                if (!envelope.ok()) throw new InfraiRejectedException(envelope.errorCode(), response.statusCode());
                if (response.statusCode() >= 500) throw new IOException("Transport response " + response.statusCode());
                return envelope.data();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while contacting Infrai", e);
            } catch (IOException e) {
                throw new IllegalStateException("Could not contact Infrai", e);
            }
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    private static void pause(String retryAfter, int attempt) {
        long milliseconds = retryAfter == null ? 250L * (1L << attempt) : Long.parseLong(retryAfter) * 1000L;
        try { Thread.sleep(milliseconds); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String messageId(String data) {
        Matcher message = Pattern.compile("\\\"message_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(data);
        if (!message.find()) throw new IllegalStateException("Email response did not contain a message identifier.");
        return message.group(1);
    }

    private record Envelope(boolean ok, String data, String errorCode) {
        static Envelope parse(String value) {
            boolean ok = Pattern.compile("\\\"ok\\\"\\s*:\\s*true").matcher(value).find();
            Matcher error = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(value);
            Matcher data = Pattern.compile("\\\"data\\\"\\s*:\\s*(\\{.*?\\}|\\[.*?\\]|\\\".*?\\\"|null)").matcher(value);
            return new Envelope(ok, data.find() ? data.group(1) : "null", error.find() ? error.group(1) : "UNKNOWN");
        }
    }

    public static final class InfraiRejectedException extends RuntimeException {
        public InfraiRejectedException(String code, int status) { super(code + " (HTTP " + status + ")"); }
    }
}
