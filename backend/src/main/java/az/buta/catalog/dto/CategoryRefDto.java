package az.buta.catalog.dto;

import java.util.UUID;

public record CategoryRefDto(UUID id, String slug, String name, String iconKey) {
}
