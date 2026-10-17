package az.buta.loyalty.task;

import az.buta.loyalty.domain.TaskType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DistinctPartnersEvaluator implements TaskEvaluator {

    @Override
    public TaskType type() {
        return TaskType.DISTINCT_PARTNERS;
    }

    @Override
    public int progress(TaskWindow window, UserFacts facts, BigDecimal minOrderAmount) {
        return (int) facts.purchases().stream()
                .filter(purchase -> window.contains(purchase.purchasedAt()) && purchase.orderTotal().compareTo(minOrderAmount) >= 0)
                .flatMap(purchase -> purchase.partnerIds().stream())
                .distinct()
                .count();
    }
}
