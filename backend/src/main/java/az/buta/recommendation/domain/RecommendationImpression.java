package az.buta.recommendation.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "recommendation_impressions")
public class RecommendationImpression extends AbstractEntity {

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String placement;

    @Column(name = "algorithm_version", nullable = false)
    private String algorithmVersion;

    @Column(nullable = false)
    private String variant;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private BigDecimal score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Double> components;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "served_at", nullable = false)
    private Instant servedAt;

    protected RecommendationImpression() {
    }

    public RecommendationImpression(String requestId, UUID userId, String placement, String algorithmVersion, String variant, UUID offerId,
                                    int position, BigDecimal score, Map<String, Double> components, String reasonCode, Instant servedAt) {
        this.requestId = requestId;
        this.userId = userId;
        this.placement = placement;
        this.algorithmVersion = algorithmVersion;
        this.variant = variant;
        this.offerId = offerId;
        this.position = position;
        this.score = score;
        this.components = components;
        this.reasonCode = reasonCode;
        this.servedAt = servedAt;
    }
}
