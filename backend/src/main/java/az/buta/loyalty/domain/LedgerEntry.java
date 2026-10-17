package az.buta.loyalty.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "loyalty_ledger_entries")
public class LedgerEntry extends AbstractEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private LedgerEntryType entryType;

    @Column(nullable = false)
    private long points;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private LedgerSourceType sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "reverses_entry_id")
    private UUID reversesEntryId;

    @Column(name = "description_key", nullable = false)
    private String descriptionKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description_params", nullable = false)
    private Map<String, String> descriptionParams;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(UUID userId, LedgerEntryType entryType, long points, String idempotencyKey, LedgerSourceType sourceType,
                       UUID sourceId, UUID reversesEntryId, String descriptionKey, Map<String, String> descriptionParams,
                       UUID actorUserId, Instant createdAt) {
        this.userId = userId;
        this.entryType = entryType;
        this.points = points;
        this.idempotencyKey = idempotencyKey;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.reversesEntryId = reversesEntryId;
        this.descriptionKey = descriptionKey;
        this.descriptionParams = descriptionParams;
        this.actorUserId = actorUserId;
        this.createdAt = createdAt;
    }

    public UUID userId() {
        return userId;
    }

    public LedgerEntryType entryType() {
        return entryType;
    }

    public long points() {
        return points;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public LedgerSourceType sourceType() {
        return sourceType;
    }

    public UUID sourceId() {
        return sourceId;
    }

    public UUID reversesEntryId() {
        return reversesEntryId;
    }

    public String descriptionKey() {
        return descriptionKey;
    }

    public Map<String, String> descriptionParams() {
        return descriptionParams;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
