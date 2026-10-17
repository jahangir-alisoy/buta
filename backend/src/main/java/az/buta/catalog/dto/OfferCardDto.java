package az.buta.catalog.dto;

import az.buta.common.money.MoneyDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OfferCardDto(
        UUID id,
        String slug,
        String offerType,
        String title,
        String subtitle,
        String imagePath,
        CategoryRefDto category,
        List<PartnerRefDto> partners,
        MoneyDto sellingPrice,
        MoneyDto referenceRetail,
        MoneyDto savings,
        String savingsPercent,
        boolean plusOnly,
        String availability,
        Integer remaining,
        Instant endsAt,
        Instant eventStartsAt,
        List<String> cities,
        boolean purchasable,
        String purchaseBlockReason,
        boolean favorite,
        Long pointsPreview,
        int componentCount,
        CampaignBadgeDto campaign) {
}
