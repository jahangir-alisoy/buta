package az.buta.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record CampaignBadgeDto(UUID id, String slug, String name, String audience, Integer bonusPoints, Instant endsAt) {
}
