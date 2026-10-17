package az.buta.auth.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge extends AbstractEntity {

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "request_ip_hash")
    private String requestIpHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OtpChallenge() {
    }

    public static OtpChallenge issue(String email, OtpPurpose purpose, UUID userId, Instant now, Instant expiresAt,
                                     int maxAttempts, String requestIpHash) {
        OtpChallenge challenge = new OtpChallenge();
        challenge.email = email;
        challenge.purpose = purpose;
        challenge.userId = userId;
        challenge.codeHash = "";
        challenge.createdAt = now;
        challenge.expiresAt = expiresAt;
        challenge.maxAttempts = maxAttempts;
        challenge.requestIpHash = requestIpHash;
        return challenge;
    }

    public void assignCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public boolean isOpen(Instant now) {
        return consumedAt == null && lockedAt == null && supersededAt == null && now.isBefore(expiresAt);
    }

    public void supersede(Instant now) {
        if (supersededAt == null && consumedAt == null) {
            supersededAt = now;
        }
    }

    public boolean registerFailedAttempt(Instant now) {
        attemptCount++;
        if (attemptCount >= maxAttempts) {
            lockedAt = now;
            return true;
        }
        return false;
    }

    public void consume(Instant now) {
        consumedAt = now;
    }

    public String email() {
        return email;
    }

    public OtpPurpose purpose() {
        return purpose;
    }

    public UUID userId() {
        return userId;
    }

    public String codeHash() {
        return codeHash;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
