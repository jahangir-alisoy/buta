package az.buta.catalog;

import az.buta.pricing.LoyaltySettings;
import az.buta.pricing.PricingSettings;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PricingContext(Instant now, PricingSettings pricing, LoyaltySettings loyalty, BigDecimal highestTierMultiplier,
                             Map<UUID, Integer> campaignBonusByOffer, Map<UUID, CampaignRef> campaignByOffer) {

    public int bonusFor(UUID offerId) {
        return campaignBonusByOffer.getOrDefault(offerId, 0);
    }
}
