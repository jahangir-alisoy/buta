package az.buta.catalog.dto;

import java.util.UUID;

public record CategoryDto(UUID id, String slug, String name, String iconKey, int offerCount) {
}
