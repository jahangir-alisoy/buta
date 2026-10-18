package az.buta.payment;

import az.buta.common.domain.AbstractEntity;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment extends AbstractEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_reference", nullable = false)
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "AZN";

    @Column(name = "refunded_amount", nullable = false)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Payment() {
    }

    public static Payment initiate(UUID orderId, String provider, String providerReference, BigDecimal amount, Instant now) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.provider = provider;
        payment.providerReference = providerReference;
        payment.status = PaymentStatus.PENDING;
        payment.amount = amount;
        payment.createdAt = now;
        payment.updatedAt = now;
        return payment;
    }

    public void capture(Instant now) {
        requirePending();
        status = PaymentStatus.CAPTURED;
        capturedAt = now;
        updatedAt = now;
    }

    public void assignProviderReference(String providerReference) {
        requirePending();
        this.providerReference = providerReference;
    }

    public void fail(String reason, Instant now) {
        requirePending();
        status = PaymentStatus.FAILED;
        failedAt = now;
        failureReason = reason;
        updatedAt = now;
    }

    public void abandon(Instant now) {
        if (status == PaymentStatus.PENDING) {
            fail("ABANDONED", now);
        }
    }

    public void recordRefund(BigDecimal refunded, Instant now) {
        if (status != PaymentStatus.CAPTURED && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw ApiException.of(ErrorCode.ORDER_STATE_INVALID);
        }
        refundedAmount = refundedAmount.add(refunded);
        status = refundedAmount.compareTo(amount) >= 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        updatedAt = now;
    }

    private void requirePending() {
        if (status != PaymentStatus.PENDING) {
            throw ApiException.of(ErrorCode.ORDER_STATE_INVALID);
        }
    }

    public UUID orderId() {
        return orderId;
    }

    public String provider() {
        return provider;
    }

    public String providerReference() {
        return providerReference;
    }

    public PaymentStatus status() {
        return status;
    }

    public BigDecimal amount() {
        return amount;
    }

    public BigDecimal refundedAmount() {
        return refundedAmount;
    }
}
