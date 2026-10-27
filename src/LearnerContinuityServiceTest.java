import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Focused business check; run with: javac src/*.java && java -ea -cp src LearnerContinuityServiceTest */
public final class LearnerContinuityServiceTest {
    public static void main(String[] args) {
        LearnerContinuityService service = new LearnerContinuityService();
        LearnerContinuityService.CourseCohort cohort = new LearnerContinuityService.CourseCohort("Biology", List.of(
                new LearnerContinuityService.Learner("Asha", LocalDate.now().plusDays(2)),
                new LearnerContinuityService.Learner("Rui", LocalDate.now().plusDays(10))));

        LearnerContinuityService.CourseReport result = service.assess(cohort, new BigDecimal("4.00"), new BigDecimal("5.00"));
        assert result.rechargeProtectionNeeded() : "low balance with a near deadline should protect delivery";
        assert result.approachingDeadlines() == 1 : "only the imminent learner deadline belongs in the report";
    }
}
