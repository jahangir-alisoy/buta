package az.buta.catalog;

import az.buta.catalog.domain.PartnerProduct;
import az.buta.catalog.dto.CampaignBadgeDto;
import az.buta.catalog.dto.CategoryRefDto;
import az.buta.catalog.dto.ComponentDto;
import az.buta.catalog.dto.OfferCardDto;
import az.buta.catalog.dto.PartnerRefDto;
import az.buta.common.money.MoneyDto;
import az.buta.membership.MembershipFeature;
import az.buta.pricing.BusinessSettingsService;
import az.buta.pricing.LoyaltySettings;
import az.buta.pricing.PriceCalculator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class OfferViewAssembler {

    private final BusinessSettingsService settings;

    public OfferViewAssembler(BusinessSettingsService settings) {
        this.settings = settings;
    }

    public List<OfferCardDto> cards(List<OfferSnapshot> snapshots, Viewer viewer) {
        int threshold = settings.pricing().lowStockThreshold();
        LoyaltySettings loyalty = settings.loyalty();
        return snapshots.stream().map(snapshot -> card(snapshot, viewer, threshold, loyalty)).toList();
    }

    public OfferCardDto card(OfferSnapshot snapshot, Viewer viewer) {
        return card(snapshot, viewer, settings.pricing().lowStockThreshold(), settings.loyalty());
    }

    private OfferCardDto card(OfferSnapshot snapshot, Viewer viewer, int lowStockThreshold, LoyaltySettings loyalty) {
        Locale locale = viewer.locale();
        String availability = snapshot.available() <= 0 ? "SOLD_OUT" : snapshot.available() <= lowStockThreshold ? "LOW_STOCK" : "AVAILABLE";
        String blockReason = blockReason(snapshot, viewer);
        boolean earnsPoints = viewer.has(MembershipFeature.LOYALTY_POINTS);
        Long pointsPreview = earnsPoints
                ? PriceCalculator.maxPoints(snapshot.sellingPrice(), loyalty.pointsPerAzn(), viewer.tierMultiplier()) + snapshot.campaignBonusPoints()
                : null;
        CampaignBadgeDto campaign = snapshot.campaign() == null ? null : new CampaignBadgeDto(snapshot.campaign().id(),
                snapshot.campaign().slug(), snapshot.campaign().name().in(locale), snapshot.campaign().audience(),
                earnsPoints && snapshot.campaignBonusPoints() > 0 ? snapshot.campaignBonusPoints() : null, snapshot.campaign().endsAt());
        return new OfferCardDto(snapshot.id(), snapshot.slug(), snapshot.type().name(), snapshot.title().in(locale),
                snapshot.subtitle().in(locale), snapshot.imagePath(),
                new CategoryRefDto(snapshot.primaryCategory().id(), snapshot.primaryCategory().slug(),
                        snapshot.primaryCategory().name().in(locale), snapshot.primaryCategory().iconKey()),
                snapshot.partners().stream().map(partner -> new PartnerRefDto(partner.id(), partner.slug(), partner.name(), partner.demo())).toList(),
                MoneyDto.of(snapshot.sellingPrice()), MoneyDto.of(snapshot.pricing().referenceRetail()),
                MoneyDto.of(snapshot.pricing().savings().max(java.math.BigDecimal.ZERO)), snapshot.pricing().savingsPercent().toPlainString(),
                snapshot.plusOnly(), availability, "LOW_STOCK".equals(availability) ? snapshot.available() : null,
                snapshot.endsAt(), snapshot.eventStartsAt(), List.copyOf(snapshot.cities()),
                blockReason == null, blockReason, viewer.isFavorite(snapshot.id()), pointsPreview, snapshot.componentCount(), campaign);
    }

    public ComponentDto component(PartnerProduct product, int quantity, Locale locale) {
        return new ComponentDto(product.getId(), product.title().in(locale), product.description().in(locale),
                new PartnerRefDto(product.partner().getId(), product.partner().slug(), product.partner().name(), product.partner().demo()),
                new CategoryRefDto(product.category().getId(), product.category().slug(), product.category().name().in(locale),
                        product.category().iconKey()),
                product.productType().name(), quantity, MoneyDto.of(product.retailPrice()), product.validFrom(), product.validUntil(),
                product.eventStartsAt(), product.eventEndsAt(), product.venueName(), product.venueAddress(), product.city(),
                product.redemptionMethod().name(), product.redemptionInstructions().in(locale), product.terms().in(locale),
                product.eligibilityNote() == null ? null : product.eligibilityNote().in(locale), product.refundable(),
                product.refundDeadlineHours());
    }

    public static String blockReason(OfferSnapshot snapshot, Viewer viewer) {
        if (!snapshot.sellable()) {
            return "NOT_SELLABLE";
        }
        if (snapshot.available() <= 0) {
            return "SOLD_OUT";
        }
        if (snapshot.plusOnly() && !viewer.has(MembershipFeature.EXCLUSIVE_OFFERS)) {
            return "PLUS_REQUIRED";
        }
        return null;
    }
}
