package az.buta.catalog.admin;

import az.buta.common.i18n.LocalizedText;
import az.buta.pricing.PriceBreakdown;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminCatalogDtos {

    private AdminCatalogDtos() {
    }

    public record CategoryView(UUID id, String slug, LocalizedText name, String iconKey, int sortOrder, boolean active) {
    }

    public record PartnerView(UUID id, String slug, String name, LocalizedText description, String city, String address,
                              String websiteUrl, String logoPath, boolean demo, String status) {
    }

    public record ProductView(UUID id, UUID partnerId, String partnerName, UUID categoryId, String productType, LocalizedText title,
                              LocalizedText description, LocalizedText terms, LocalizedText redemptionInstructions,
                              LocalizedText eligibilityNote, BigDecimal retailPrice, String costModel, BigDecimal unitNetCost,
                              BigDecimal commissionPercent, Instant validFrom, Instant validUntil, Instant eventStartsAt,
                              Instant eventEndsAt, String venueName, String venueAddress, String city, String redemptionMethod,
                              boolean refundable, int refundDeadlineHours, boolean restockOnRefund, String status,
                              int totalQuantity, int reservedQuantity, int soldQuantity, int available) {
    }

    public record ComponentView(UUID productId, String productTitle, String partnerName, int quantity, BigDecimal negotiatedUnitCost) {
    }

    public record OfferView(UUID id, String slug, String offerType, String status, String visibility, UUID primaryCategoryId,
                            LocalizedText title, LocalizedText subtitle, LocalizedText description, String imagePath,
                            BigDecimal sellingPrice, Instant saleStartsAt, Instant saleEndsAt, int maxPerOrder, int businessPriority,
                            boolean featured, List<String> tags, List<ComponentView> components, PriceBreakdown breakdown,
                            int available, Instant publishedAt) {
    }

    public record CampaignView(UUID id, String slug, LocalizedText name, LocalizedText bannerText, String audience, Instant startsAt,
                               Instant endsAt, int bonusPointsPerUnit, boolean featured, String status, List<UUID> offerIds) {
    }

    public record PartnerPerformance(UUID partnerId, String partnerName, Instant from, Instant to, long unitsSold,
                                     BigDecimal partnerPayout, long refundedUnits, long vouchersIssued, long vouchersRedeemed,
                                     String redemptionRatePercent, List<ProductPerformance> products) {
    }

    public record ProductPerformance(UUID productId, String title, int totalQuantity, int soldQuantity, String sellThroughPercent) {
    }

    public record PublishFailure(String rule) {
    }
}
