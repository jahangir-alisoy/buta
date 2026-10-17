package az.buta.catalog;

import az.buta.common.i18n.LocalizedText;

import java.time.Instant;
import java.util.UUID;

public record CampaignRef(UUID id, String slug, LocalizedText name, LocalizedText bannerText, String audience, int bonusPointsPerUnit,
                          Instant endsAt, boolean featured) {
}
