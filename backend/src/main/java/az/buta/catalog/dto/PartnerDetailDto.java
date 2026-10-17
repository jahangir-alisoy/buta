package az.buta.catalog.dto;

import java.util.List;
import java.util.UUID;

public record PartnerDetailDto(UUID id, String slug, String name, String description, String city, String address,
                               String websiteUrl, boolean demo, String logoPath, List<OfferCardDto> offers) {
}
