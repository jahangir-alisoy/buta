package az.buta.catalog.dto;

import java.time.Instant;
import java.util.List;

public record OfferDetailDto(OfferCardDto offer, String description, List<String> tags, List<ComponentDto> components,
                             Instant validFrom, Instant validUntil, int maxPerOrder) {
}
