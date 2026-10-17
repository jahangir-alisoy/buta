package az.buta.recommendation.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interaction_events")
public class InteractionEvent extends AbstractEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private InteractionType eventType;

    @Column(name = "offer_id")
    private UUID offerId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "query_text")
    private String queryText;

    @Column(name = "recommendation_request_id")
    private String recommendationRequestId;

    private String placement;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected InteractionEvent() {
    }

    public InteractionEvent(UUID userId, InteractionType eventType, UUID offerId, UUID categoryId, String queryText,
                            String recommendationRequestId, String placement, Instant occurredAt) {
        this.userId = userId;
        this.eventType = eventType;
        this.offerId = offerId;
        this.categoryId = categoryId;
        this.queryText = queryText;
        this.recommendationRequestId = recommendationRequestId;
        this.placement = placement;
        this.occurredAt = occurredAt;
    }

    public UUID userId() {
        return userId;
    }

    public InteractionType eventType() {
        return eventType;
    }

    public UUID offerId() {
        return offerId;
    }

    public UUID categoryId() {
        return categoryId;
    }

    public Instant occurredAt() {
        return occurredAt;
    }
}
