import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Explanatory entry point, shaped like a controller coordinating configuration and domain services. */
public final class CourseContinuityApplication {
    public static void main(String[] args) {
        CourseContinuityConfiguration config = CourseContinuityConfiguration.fromEnvironment();
        InfraiCourseClient infrai = new InfraiCourseClient(config.apiKey());
        LearnerContinuityService continuity = new LearnerContinuityService();
        LearnerContinuityService.CourseCohort cohort = new LearnerContinuityService.CourseCohort("Algebra I", List.of(
                new LearnerContinuityService.Learner("Mina", LocalDate.now().plusDays(1)),
                new LearnerContinuityService.Learner("Owen", LocalDate.now().plusDays(8))));

        // One credential covers the balance policy and the educator message.
        infrai.account.autorecharge.configure(config.triggerBalance().toPlainString(), config.rechargeAmount().toPlainString());
        String balanceEnvelope = infrai.account.balance();
        BigDecimal balance = BalanceReader.usd(balanceEnvelope);
        LearnerContinuityService.CourseReport report = continuity.assess(cohort, balance, config.triggerBalance());

        if (report.rechargeProtectionNeeded()) {
            String message = infrai.email.send(config.educatorEmail(), "Recharge protects " + report.courseName(),
                    "<p>" + report.approachingDeadlines() + " learner deadlines are within three days.</p>");
            System.out.println("Educator report sent: " + message);
        } else {
            System.out.println("Course delivery remains covered; no educator message is needed.");
        }
    }

    static final class BalanceReader {
        static BigDecimal usd(String envelopeData) {
            java.util.regex.Matcher amount = java.util.regex.Pattern
                    .compile("\\\"balance\\\"\\s*:\\s*([0-9.]+)").matcher(envelopeData);
            if (!amount.find()) throw new IllegalStateException("Balance response did not contain a balance value.");
            return new BigDecimal(amount.group(1));
        }
    }
}
