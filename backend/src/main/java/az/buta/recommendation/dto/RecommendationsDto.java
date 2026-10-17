package az.buta.recommendation.dto;

import az.buta.catalog.dto.OfferCardDto;

import java.util.List;
import java.util.Map;

public record RecommendationsDto(String requestId, String placement, boolean personalized, String algorithmVersion, String variant,
                                 List<Item> items) {

    public record Item(OfferCardDto offer, String reasonCode, Map<String, String> reasonParams, boolean exploration) {
    }
}
