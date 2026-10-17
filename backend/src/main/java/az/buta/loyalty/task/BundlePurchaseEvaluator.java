package az.buta.loyalty.task;

import az.buta.loyalty.domain.TaskType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BundlePurchaseEvaluator implements TaskEvaluator {

    @Override
    public TaskType type() {
        return TaskType.BUNDLE_PURCHASE;
    }

    @Override
    public int progress(TaskWindow window, UserFacts facts, BigDecimal minOrderAmount) {
        return (int) facts.purchases().stream()
                .filter(purchase -> purchase.bundle() && window.contains(purchase.purchasedAt())
                        && purchase.orderTotal().compareTo(minOrderAmount) >= 0)
                .count();
    }
}
