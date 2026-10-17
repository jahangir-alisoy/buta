package az.buta.catalog.dto;

import java.util.List;

public record HomeDto(CampaignSectionDto featuredCampaign, List<OfferCardDto> popular, List<OfferCardDto> bestValueBundles,
                      List<OfferCardDto> thisWeek, List<OfferCardDto> endingSoon, List<CategoryDto> categories) {
}
