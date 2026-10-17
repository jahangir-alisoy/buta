package az.buta.audit;

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
@Table(name = "audit_logs")
public class AuditLog extends AbstractEntity {

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    private ActorType actorType;

    @Column(nullable = false)
    private String action;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private String targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> details;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "ip_hash")
    private String ipHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(UUID actorUserId, ActorType actorType, String action, String targetType, String targetId,
                    Map<String, Object> details, String requestId, String ipHash, Instant createdAt) {
        this.actorUserId = actorUserId;
        this.actorType = actorType;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.requestId = requestId;
        this.ipHash = ipHash;
        this.createdAt = createdAt;
    }

    public UUID actorUserId() {
        return actorUserId;
    }

    public ActorType actorType() {
        return actorType;
    }

    public String action() {
        return action;
    }

    public String targetType() {
        return targetType;
    }

    public String targetId() {
        return targetId;
    }

    public Map<String, Object> details() {
        return details;
    }

    public String requestId() {
        return requestId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
