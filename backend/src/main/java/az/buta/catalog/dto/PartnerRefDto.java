package az.buta.catalog.dto;

import java.util.UUID;

public record PartnerRefDto(UUID id, String slug, String name, boolean demo) {
}
