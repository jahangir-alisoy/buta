package az.buta.loyalty.task;

import az.buta.loyalty.domain.TaskType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ReferralEvaluator implements TaskEvaluator {

    @Override
    public TaskType type() {
        return TaskType.REFERRAL;
    }

    @Override
    public int progress(TaskWindow window, UserFacts facts, BigDecimal minOrderAmount) {
        return (int) facts.qualifiedReferrals().stream().filter(window::contains).count();
    }
}
