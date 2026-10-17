package az.buta.order.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refunds")
public class Refund extends AbstractEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiated_by", nullable = false)
    private RefundInitiator initiatedBy;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(nullable = false)
    private String status;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Refund() {
    }

    public Refund(UUID orderId, UUID paymentId, BigDecimal amount, String reason, RefundInitiator initiatedBy, UUID actorUserId,
                  String status, String providerReference, Instant createdAt) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
        this.initiatedBy = initiatedBy;
        this.actorUserId = actorUserId;
        this.status = status;
        this.providerReference = providerReference;
        this.createdAt = createdAt;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String reason() {
        return reason;
    }

    public RefundInitiator initiatedBy() {
        return initiatedBy;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
