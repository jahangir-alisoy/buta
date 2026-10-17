package az.buta.auth.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
public class UserSession extends AbstractEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "idle_expires_at", nullable = false)
    private Instant idleExpiresAt;

    @Column(name = "absolute_expires_at", nullable = false)
    private Instant absoluteExpiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoke_reason")
    private String revokeReason;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_hash")
    private String ipHash;

    protected UserSession() {
    }

    public static UserSession open(UUID userId, String tokenHash, Instant now, Duration idleTimeout, Duration absoluteTimeout,
                                   String userAgent, String ipHash) {
        UserSession session = new UserSession();
        session.userId = userId;
        session.tokenHash = tokenHash;
        session.createdAt = now;
        session.lastSeenAt = now;
        session.absoluteExpiresAt = now.plus(absoluteTimeout);
        session.idleExpiresAt = earliest(now.plus(idleTimeout), session.absoluteExpiresAt);
        session.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(200, userAgent.length()));
        session.ipHash = ipHash;
        return session;
    }

    public boolean isUsable(Instant now) {
        return revokedAt == null && now.isBefore(idleExpiresAt) && now.isBefore(absoluteExpiresAt);
    }

    public boolean touch(Instant now, Duration idleTimeout, Duration touchInterval) {
        if (Duration.between(lastSeenAt, now).compareTo(touchInterval) < 0) {
            return false;
        }
        lastSeenAt = now;
        idleExpiresAt = earliest(now.plus(idleTimeout), absoluteExpiresAt);
        return true;
    }

    public void revoke(Instant now, String reason) {
        if (revokedAt == null) {
            revokedAt = now;
            revokeReason = reason;
        }
    }

    public UUID userId() {
        return userId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    private static Instant earliest(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }
}
