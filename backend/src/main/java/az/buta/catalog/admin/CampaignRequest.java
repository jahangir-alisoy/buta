package az.buta.catalog.admin;

import az.buta.catalog.domain.ActiveStatus;
import az.buta.catalog.domain.CampaignAudience;
import az.buta.common.i18n.LocalizedText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CampaignRequest(@Pattern(regexp = "^[a-z0-9-]{2,80}$") String slug, @NotNull @Valid LocalizedText name,
                              @NotNull @Valid LocalizedText bannerText, @NotNull CampaignAudience audience, @NotNull Instant startsAt,
                              @NotNull Instant endsAt, @Min(0) @Max(1000) int bonusPointsPerUnit, boolean featured,
                              @NotNull ActiveStatus status, @NotNull @Size(max = 50) Set<UUID> offerIds) {
}
