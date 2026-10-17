package az.buta.catalog;

import az.buta.catalog.domain.Campaign;
import az.buta.catalog.domain.CampaignRepository;
import az.buta.catalog.domain.Offer;
import az.buta.catalog.domain.OfferComponent;
import az.buta.catalog.domain.PartnerProduct;
import az.buta.loyalty.domain.LoyaltyTier;
import az.buta.loyalty.domain.LoyaltyTierRepository;
import az.buta.pricing.BusinessSettingsService;
import az.buta.pricing.PriceBreakdown;
import az.buta.pricing.PriceCalculator;
import az.buta.pricing.PricingComponent;
import az.buta.pricing.PricingInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OfferPricingService {

    private final PriceCalculator calculator;
    private final BusinessSettingsService settings;
    private final LoyaltyTierRepository tiers;
    private final CampaignRepository campaigns;
    private final Clock clock;

    public OfferPricingService(PriceCalculator calculator, BusinessSettingsService settings, LoyaltyTierRepository tiers,
                               CampaignRepository campaigns, Clock clock) {
        this.calculator = calculator;
        this.settings = settings;
        this.tiers = tiers;
        this.campaigns = campaigns;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PricingContext context() {
        Instant now = clock.instant();
        BigDecimal highest = tiers.findAll().stream().map(LoyaltyTier::earnMultiplier).max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);
        Map<UUID, Integer> bonus = new HashMap<>();
        Map<UUID, CampaignRef> byOffer = new HashMap<>();
        for (Campaign campaign : campaigns.findRunning(now)) {
            CampaignRef ref = new CampaignRef(campaign.getId(), campaign.slug(), campaign.name(), campaign.bannerText(),
                    campaign.audience().name(), campaign.bonusPointsPerUnit(), campaign.endsAt(), campaign.featured());
            campaign.offers().forEach(offer -> {
                bonus.merge(offer.getId(), campaign.bonusPointsPerUnit(), Math::max);
                byOffer.merge(offer.getId(), ref, (left, right) -> left.bonusPointsPerUnit() >= right.bonusPointsPerUnit() ? left : right);
            });
        }
        return new PricingContext(now, settings.pricing(), settings.loyalty(), highest, bonus, byOffer);
    }

    public PriceBreakdown breakdown(Offer offer, PricingContext context) {
        return breakdown(offer.sellingPrice(), offer.components(), context.bonusFor(offer.getId()), context);
    }

    public PriceBreakdown breakdown(BigDecimal sellingPrice, List<OfferComponent> components, int bonusPoints, PricingContext context) {
        List<PricingComponent> inputs = components.stream()
                .map(component -> toInput(component.product(), component.quantity(), component.negotiatedUnitCost()))
                .toList();
        return calculator.calculate(new PricingInput(sellingPrice, inputs, context.pricing(), context.loyalty(),
                context.highestTierMultiplier(), bonusPoints));
    }

    public PriceBreakdown breakdownFor(BigDecimal sellingPrice, List<ProductQuantity> products, int bonusPoints, PricingContext context) {
        List<PricingComponent> inputs = products.stream()
                .map(item -> toInput(item.product(), item.quantity(), item.negotiatedUnitCost()))
                .toList();
        return calculator.calculate(new PricingInput(sellingPrice, inputs, context.pricing(), context.loyalty(),
                context.highestTierMultiplier(), bonusPoints));
    }

    private static PricingComponent toInput(PartnerProduct product, int quantity, BigDecimal negotiatedUnitCost) {
        return new PricingComponent(product.getId(), product.partner().getId(), product.retailPrice(), quantity, product.costModel(),
                product.unitNetCost(), product.commissionPercent(), negotiatedUnitCost);
    }

    public record ProductQuantity(PartnerProduct product, int quantity, BigDecimal negotiatedUnitCost) {
    }
}
