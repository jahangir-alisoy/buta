package az.buta.loyalty.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referrals")
public class Referral extends AbstractEntity {

    @Column(name = "referrer_user_id", nullable = false)
    private UUID referrerUserId;

    @Column(name = "referee_user_id", nullable = false)
    private UUID refereeUserId;

    @Column(name = "code_used", nullable = false)
    private String codeUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReferralStatus status;

    @Column(name = "qualifying_order_id")
    private UUID qualifyingOrderId;

    @Column(name = "qualified_at")
    private Instant qualifiedAt;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Referral() {
    }

    public Referral(UUID referrerUserId, UUID refereeUserId, String codeUsed, Instant now) {
        this.referrerUserId = referrerUserId;
        this.refereeUserId = refereeUserId;
        this.codeUsed = codeUsed;
        this.status = ReferralStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void qualify(UUID orderId, Instant now) {
        this.status = ReferralStatus.QUALIFIED;
        this.qualifyingOrderId = orderId;
        this.qualifiedAt = now;
        this.updatedAt = now;
    }

    public void holdForReview(UUID orderId, String note, Instant now) {
        this.status = ReferralStatus.UNDER_REVIEW;
        this.qualifyingOrderId = orderId;
        this.reviewNote = note;
        this.updatedAt = now;
    }

    public void approve(UUID adminId, Instant now) {
        this.status = ReferralStatus.QUALIFIED;
        this.qualifiedAt = now;
        this.reviewedBy = adminId;
        this.updatedAt = now;
    }

    public void reject(UUID adminId, String note, Instant now) {
        this.status = ReferralStatus.REJECTED;
        this.reviewedBy = adminId;
        this.reviewNote = note;
        this.updatedAt = now;
    }

    public void reverse(Instant now) {
        this.status = ReferralStatus.REVERSED;
        this.updatedAt = now;
    }

    public UUID referrerUserId() {
        return referrerUserId;
    }

    public UUID refereeUserId() {
        return refereeUserId;
    }

    public ReferralStatus status() {
        return status;
    }

    public UUID qualifyingOrderId() {
        return qualifyingOrderId;
    }

    public Instant qualifiedAt() {
        return qualifiedAt;
    }

    public String reviewNote() {
        return reviewNote;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
