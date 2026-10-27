import java.math.BigDecimal;

/** Configuration layer: environment values stay at the application boundary. */
public final class CourseContinuityConfiguration {
    private final String apiKey;
    private final String educatorEmail;
    private final BigDecimal triggerBalance;
    private final BigDecimal rechargeAmount;

    private CourseContinuityConfiguration(String apiKey, String educatorEmail,
                                          BigDecimal triggerBalance, BigDecimal rechargeAmount) {
        this.apiKey = apiKey;
        this.educatorEmail = educatorEmail;
        this.triggerBalance = triggerBalance;
        this.rechargeAmount = rechargeAmount;
    }

    public static CourseContinuityConfiguration fromEnvironment() {
        String apiKey = required("INFRAI_API_KEY");
        String educatorEmail = required("EDUCATOR_EMAIL");
        return new CourseContinuityConfiguration(apiKey, educatorEmail,
                new BigDecimal(System.getenv().getOrDefault("RECHARGE_TRIGGER_USD", "5.00")),
                new BigDecimal(System.getenv().getOrDefault("RECHARGE_AMOUNT_USD", "25.00")));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + name + " before starting the course continuity example.");
        }
        return value;
    }

    public String apiKey() { return apiKey; }
    public String educatorEmail() { return educatorEmail; }
    public BigDecimal triggerBalance() { return triggerBalance; }
    public BigDecimal rechargeAmount() { return rechargeAmount; }
}
