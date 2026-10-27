import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Domain layer for the educator-facing decision, independent of HTTP. */
public final class LearnerContinuityService {
    public CourseReport assess(CourseCohort cohort, BigDecimal balance, BigDecimal triggerBalance) {
        long approachingDeadlines = cohort.learners().stream()
                .filter(learner -> !learner.deadline().isBefore(LocalDate.now()))
                .filter(learner -> !learner.deadline().isAfter(LocalDate.now().plusDays(3)))
                .count();
        boolean protectDelivery = balance.compareTo(triggerBalance) <= 0 && approachingDeadlines > 0;
        return new CourseReport(cohort.courseName(), cohort.learners().size(), approachingDeadlines, protectDelivery);
    }

    public record Learner(String name, LocalDate deadline) { }
    public record CourseCohort(String courseName, List<Learner> learners) { }
    public record CourseReport(String courseName, int enrolledLearners, long approachingDeadlines,
                               boolean rechargeProtectionNeeded) { }
}
