package az.buta.recommendation;

import az.buta.catalog.OfferSnapshot;
import az.buta.pricing.PriceCalculator;
import az.buta.pricing.RecommendationSettings;
import az.buta.recommendation.domain.InterestProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RecommendationScorer {

    public static final String INTEREST = "interest";
    public static final String BUDGET = "budget";
    public static final String VALUE = "value";
    public static final String LOYALTY = "loyalty";
    public static final String DISCOVERY = "discovery";
    public static final String BUSINESS = "business";

    public ScoredOffer score(OfferSnapshot offer, InterestProfile profile, LoyaltyContext loyalty, RecommendationSettings.Weights weights,
                             int minRelevance, long recentServes, int maxServes) {
        double interest = interest(offer, profile);
        double budget = budget(offer.sellingPrice(), profile);
        double value = Math.min(100.0, offer.pricing().savingsPercent().doubleValue() / 40.0 * 100.0);
        LoyaltyMatch loyaltyMatch = interest >= minRelevance ? loyalty(offer, profile, loyalty) : LoyaltyMatch.NONE;
        double discovery = offer.categoryIds().stream().noneMatch(profile.purchasedCategoryIds()::contains) ? 100.0 : 0.0;
        double business = Math.max(0, Math.min(100, offer.businessPriority()));
        Map<String, Double> components = new LinkedHashMap<>();
        components.put(INTEREST, round(interest));
        components.put(BUDGET, round(budget));
        components.put(VALUE, round(value));
        components.put(LOYALTY, round(loyaltyMatch.score()));
        components.put(DISCOVERY, discovery);
        components.put(BUSINESS, business);
        double score = (weights.interest() * interest + weights.budget() * budget + weights.value() * value
                + weights.loyalty() * loyaltyMatch.score() + weights.discovery() * discovery + weights.business() * business) / 100.0;
        if (recentServes >= maxServes) {
            score *= 0.5;
        }
        return new ScoredOffer(offer, components, round(score), reason(offer, profile, interest, budget, value, loyaltyMatch), false);
    }

    static double interest(OfferSnapshot offer, InterestProfile profile) {
        double category = offer.categoryIds().stream().mapToDouble(id -> profile.categoryAffinity().getOrDefault(id, 0.0)).max().orElse(0);
        double partner = offer.partners().stream().mapToDouble(ref -> profile.partnerAffinity().getOrDefault(ref.id(), 0.0)).max().orElse(0);
        return Math.min(100.0, category * 0.85 + partner * 0.15);
    }

    static double budget(BigDecimal price, InterestProfile profile) {
        if (!profile.hasBudget()) {
            return 50.0;
        }
        double low = profile.priceLow().doubleValue();
        double high = profile.priceHigh().doubleValue();
        double value = price.doubleValue();
        if (value >= low && value <= high) {
            return 100.0;
        }
        if (value < low && profile.priceSensitivity() >= 0.5) {
            return 100.0;
        }
        double width = Math.max(high - low, 1.0);
        double distance = value < low ? low - value : value - high;
        return Math.max(0.0, 100.0 * (1.0 - distance / (2.0 * width)));
    }

    private static LoyaltyMatch loyalty(OfferSnapshot offer, InterestProfile profile, LoyaltyContext loyalty) {
        if (!loyalty.earnsPoints()) {
            return LoyaltyMatch.NONE;
        }
        long estimate = PriceCalculator.maxPoints(offer.sellingPrice(), loyalty.pointsPerAzn(), loyalty.multiplier()) + offer.campaignBonusPoints();
        if (loyalty.pointsToNextTier() > 0 && estimate >= loyalty.pointsToNextTier()) {
            return new LoyaltyMatch(100.0, "REASON_NEXT_BENEFIT", Map.of("points", String.valueOf(estimate),
                    "needed", String.valueOf(loyalty.pointsToNextTier()), "tier", String.valueOf(loyalty.nextTierName())));
        }
        LoyaltyMatch best = LoyaltyMatch.NONE;
        for (LoyaltyContext.OpenTask task : loyalty.openTasks()) {
            boolean advances = switch (task.type()) {
                case BUNDLE_PURCHASE -> offer.type() == az.buta.catalog.domain.OfferType.BUNDLE;
                case NEW_CATEGORY -> !profile.purchasedCategoryIds().isEmpty()
                        && offer.categoryIds().stream().noneMatch(profile.purchasedCategoryIds()::contains);
                case DISTINCT_PARTNERS -> offer.partners().stream().anyMatch(ref -> !task.partnersInPeriod().contains(ref.id()));
                default -> false;
            };
            if (advances) {
                double score = task.progress() + 1 >= task.target() ? 100.0 : 60.0;
                if (score > best.score()) {
                    best = new LoyaltyMatch(score, "REASON_TASK_PROGRESS", Map.of("task", task.title(), "points", String.valueOf(task.rewardPoints())));
                }
            }
        }
        return best;
    }

    private static ScoredOffer.Reason reason(OfferSnapshot offer, InterestProfile profile, double interest, double budget, double value,
                                             LoyaltyMatch loyalty) {
        if (loyalty.score() >= 100.0 && loyalty.code() != null) {
            return new ScoredOffer.Reason(loyalty.code(), loyalty.params());
        }
        if (interest >= 50.0) {
            UUID best = offer.categoryIds().stream()
                    .max(java.util.Comparator.comparingDouble(id -> profile.categoryAffinity().getOrDefault(id, 0.0)))
                    .orElse(offer.primaryCategory().id());
            String slug = best.equals(offer.primaryCategory().id()) ? offer.primaryCategory().slug() : "";
            return new ScoredOffer.Reason("REASON_CATEGORY_INTEREST", Map.of("categoryId", best.toString(), "category", slug));
        }
        if (value >= 50.0) {
            return new ScoredOffer.Reason("REASON_GREAT_VALUE", Map.of("savingsPercent", offer.pricing().savingsPercent().stripTrailingZeros().toPlainString()));
        }
        if (budget >= 90.0 && profile.hasBudget()) {
            return new ScoredOffer.Reason("REASON_BUDGET_FIT", Map.of());
        }
        if (loyalty.code() != null) {
            return new ScoredOffer.Reason(loyalty.code(), loyalty.params());
        }
        return new ScoredOffer.Reason("REASON_FOR_YOU", Map.of());
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record LoyaltyMatch(double score, String code, Map<String, String> params) {
        static final LoyaltyMatch NONE = new LoyaltyMatch(0.0, null, Map.of());
    }
}
