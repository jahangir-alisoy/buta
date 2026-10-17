package az.buta.recommendation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record InteractionBatch(@NotNull @Size(max = 50) List<@Valid Event> events) {

    public record Event(@NotNull ClientEventType type, UUID offerId, UUID categoryId, @Size(max = 100) String query,
                        @Size(max = 64) String requestId, @Size(max = 20) String placement) {
    }

    public enum ClientEventType {
        DETAIL_VIEW, SEARCH, CATEGORY_VIEW, RECOMMENDATION_CLICK
    }
}
