package az.buta.pricing;

import java.math.BigDecimal;
import java.util.List;

public record PricingInput(BigDecimal sellingPrice, List<PricingComponent> components, PricingSettings pricing,
                           LoyaltySettings loyalty, BigDecimal highestTierMultiplier, int maxCampaignBonusPoints) {
}
