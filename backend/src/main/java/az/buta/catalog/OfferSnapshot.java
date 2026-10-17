package az.buta.catalog;

import az.buta.catalog.domain.OfferType;
import az.buta.catalog.domain.OfferVisibility;
import az.buta.catalog.domain.ProductType;
import az.buta.common.i18n.LocalizedText;
import az.buta.pricing.PriceBreakdown;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record OfferSnapshot(
        UUID id,
        String slug,
        OfferType type,
        OfferVisibility visibility,
        CategoryRef primaryCategory,
        List<UUID> categoryIds,
        List<PartnerRef> partners,
        LocalizedText title,
        LocalizedText subtitle,
        String imagePath,
        PriceBreakdown pricing,
        boolean componentsUsable,
        int available,
        int maxPerOrder,
        Instant saleEndsAt,
        Instant validUntil,
        Instant eventStartsAt,
        Set<String> cities,
        Set<ProductType> productTypes,
        String searchKey,
        int businessPriority,
        boolean featured,
        Instant publishedAt,
        int campaignBonusPoints,
        CampaignRef campaign,
        int componentCount) {

    public BigDecimal sellingPrice() {
        return pricing.sellingPrice();
    }

    public boolean sellable() {
        return pricing.sellable() && componentsUsable;
    }

    public Instant endsAt() {
        return saleEndsAt.isBefore(validUntil) ? saleEndsAt : validUntil;
    }

    public boolean plusOnly() {
        return visibility == OfferVisibility.PLUS_ONLY;
    }

    public record CategoryRef(UUID id, String slug, LocalizedText name, String iconKey) {
    }

    public record PartnerRef(UUID id, String slug, String name, boolean demo) {
    }
}
