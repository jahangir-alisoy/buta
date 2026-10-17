package az.buta.loyalty.task;

import az.buta.loyalty.domain.TaskType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class NewCategoryEvaluator implements TaskEvaluator {

    @Override
    public TaskType type() {
        return TaskType.NEW_CATEGORY;
    }

    @Override
    public int progress(TaskWindow window, UserFacts facts, BigDecimal minOrderAmount) {
        Set<UUID> before = facts.purchases().stream()
                .filter(purchase -> purchase.purchasedAt().isBefore(window.start()))
                .map(PurchaseFact::categoryId)
                .collect(Collectors.toSet());
        if (before.isEmpty()) {
            return 0;
        }
        return (int) facts.purchases().stream()
                .filter(purchase -> window.contains(purchase.purchasedAt()) && purchase.orderTotal().compareTo(minOrderAmount) >= 0)
                .map(PurchaseFact::categoryId)
                .filter(category -> !before.contains(category))
                .distinct()
                .count();
    }
}
