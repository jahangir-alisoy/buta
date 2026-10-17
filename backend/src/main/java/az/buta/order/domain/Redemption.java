package az.buta.order.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "redemptions")
public class Redemption extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RedemptionStatus status;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "redeemed_by")
    private UUID redeemedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Redemption() {
    }

    public Redemption(OrderItem orderItem, UUID productId, String code, Instant validFrom, Instant validUntil, Instant createdAt) {
        this.orderItem = orderItem;
        this.productId = productId;
        this.code = code;
        this.status = RedemptionStatus.ISSUED;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.createdAt = createdAt;
    }

    public boolean redeem(UUID actorId, Instant now) {
        if (status == RedemptionStatus.REDEEMED) {
            return false;
        }
        if (status != RedemptionStatus.ISSUED || now.isAfter(validUntil)) {
            throw az.buta.common.error.ApiException.of(az.buta.common.error.ErrorCode.ORDER_STATE_INVALID);
        }
        status = RedemptionStatus.REDEEMED;
        redeemedAt = now;
        redeemedBy = actorId;
        return true;
    }

    public boolean expireIfDue(Instant now) {
        if (status == RedemptionStatus.ISSUED && now.isAfter(validUntil)) {
            status = RedemptionStatus.EXPIRED;
            return true;
        }
        return false;
    }

    public void voidCode(Instant now) {
        if (status == RedemptionStatus.ISSUED) {
            status = RedemptionStatus.VOIDED;
            voidedAt = now;
        }
    }

    public OrderItem orderItem() {
        return orderItem;
    }

    public UUID productId() {
        return productId;
    }

    public String code() {
        return code;
    }

    public RedemptionStatus status() {
        return status;
    }

    public Instant validFrom() {
        return validFrom;
    }

    public Instant validUntil() {
        return validUntil;
    }

    public Instant redeemedAt() {
        return redeemedAt;
    }
}
