package az.buta.catalog.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CampaignSectionDto(UUID id, String slug, String name, String bannerText, String audience, int bonusPoints,
                                 Instant endsAt, List<OfferCardDto> offers) {
}
