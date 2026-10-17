package az.buta.recommendation;

import az.buta.loyalty.domain.TaskType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record LoyaltyContext(boolean earnsPoints, long pointsToNextTier, String nextTierName, BigDecimal multiplier, int pointsPerAzn,
                             List<OpenTask> openTasks) {

    public static LoyaltyContext none() {
        return new LoyaltyContext(false, 0, null, BigDecimal.ONE, 0, List.of());
    }

    public record OpenTask(String code, String title, TaskType type, int progress, int target, long rewardPoints, Set<UUID> partnersInPeriod) {
    }
}
