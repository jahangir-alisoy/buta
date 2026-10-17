package az.buta.recommendation.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record InterestProfile(Map<UUID, Double> categoryAffinity, Map<UUID, Double> partnerAffinity, Set<UUID> purchasedCategoryIds,
                              BigDecimal priceLow, BigDecimal priceHigh, double priceSensitivity, int signals) {

    public static InterestProfile empty() {
        return new InterestProfile(Map.of(), Map.of(), Set.of(), null, null, 0, 0);
    }

    public boolean hasBudget() {
        return priceLow != null && priceHigh != null;
    }
}
