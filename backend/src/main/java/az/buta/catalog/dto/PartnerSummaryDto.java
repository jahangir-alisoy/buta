package az.buta.catalog.dto;

import java.util.UUID;

public record PartnerSummaryDto(UUID id, String slug, String name, String description, String city, boolean demo, String logoPath, int offerCount) {
}
