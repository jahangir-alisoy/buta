package az.buta.order.domain;

import az.buta.common.domain.AbstractEntity;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order extends AbstractEntity {

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private String currency = "AZN";

    @Column(name = "subtotal_amount", nullable = false)
    private BigDecimal subtotalAmount;

    @Column(name = "points_redeemed", nullable = false)
    private long pointsRedeemed;

    @Column(name = "points_discount_amount", nullable = false)
    private BigDecimal pointsDiscountAmount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "checkout_idempotency_key", nullable = false)
    private String checkoutIdempotencyKey;

    @Column(name = "checkout_fingerprint", nullable = false)
    private String checkoutFingerprint;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public static Order place(String orderNumber, UUID userId, BigDecimal subtotal, long pointsRedeemed, BigDecimal pointsDiscount,
                              BigDecimal total, String idempotencyKey, String fingerprint, Instant expiresAt, Instant now) {
        Order order = new Order();
        order.orderNumber = orderNumber;
        order.userId = userId;
        order.status = OrderStatus.PENDING_PAYMENT;
        order.subtotalAmount = subtotal;
        order.pointsRedeemed = pointsRedeemed;
        order.pointsDiscountAmount = pointsDiscount;
        order.totalAmount = total;
        order.checkoutIdempotencyKey = idempotencyKey;
        order.checkoutFingerprint = fingerprint;
        order.expiresAt = expiresAt;
        order.createdAt = now;
        order.updatedAt = now;
        return order;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void markFulfilled(Instant now) {
        require(OrderStatus.PENDING_PAYMENT);
        status = OrderStatus.FULFILLED;
        fulfilledAt = now;
        updatedAt = now;
    }

    public void cancel(String reason, Instant now) {
        require(OrderStatus.PENDING_PAYMENT);
        status = OrderStatus.CANCELLED;
        cancelledAt = now;
        cancelReason = reason;
        updatedAt = now;
    }

    public void expire(Instant now) {
        require(OrderStatus.PENDING_PAYMENT);
        status = OrderStatus.EXPIRED;
        cancelledAt = now;
        cancelReason = "RESERVATION_EXPIRED";
        updatedAt = now;
    }

    public void recordRefund(Instant now) {
        require(OrderStatus.FULFILLED, OrderStatus.PARTIALLY_REFUNDED);
        boolean allRefunded = items.stream().allMatch(OrderItem::isRefunded);
        status = allRefunded ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED;
        updatedAt = now;
    }

    public boolean completeIfSettled(Instant now) {
        if (status != OrderStatus.FULFILLED && status != OrderStatus.PARTIALLY_REFUNDED) {
            return false;
        }
        List<Redemption> all = items.stream().flatMap(item -> item.redemptions().stream()).toList();
        boolean settled = !all.isEmpty() && all.stream().allMatch(redemption -> redemption.status() != RedemptionStatus.ISSUED);
        boolean anyUsed = all.stream().anyMatch(redemption -> redemption.status() != RedemptionStatus.VOIDED);
        if (settled && anyUsed) {
            status = OrderStatus.COMPLETED;
            completedAt = now;
            updatedAt = now;
            return true;
        }
        return false;
    }

    public boolean isPaid() {
        return status == OrderStatus.FULFILLED || status == OrderStatus.COMPLETED || status == OrderStatus.PARTIALLY_REFUNDED;
    }

    private void require(OrderStatus... allowed) {
        if (Arrays.stream(allowed).noneMatch(candidate -> candidate == status)) {
            throw ApiException.of(ErrorCode.ORDER_STATE_INVALID);
        }
    }

    public String orderNumber() {
        return orderNumber;
    }

    public UUID userId() {
        return userId;
    }

    public OrderStatus status() {
        return status;
    }

    public BigDecimal subtotalAmount() {
        return subtotalAmount;
    }

    public long pointsRedeemed() {
        return pointsRedeemed;
    }

    public BigDecimal pointsDiscountAmount() {
        return pointsDiscountAmount;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public String checkoutFingerprint() {
        return checkoutFingerprint;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant fulfilledAt() {
        return fulfilledAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public Instant cancelledAt() {
        return cancelledAt;
    }

    public String cancelReason() {
        return cancelReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<OrderItem> items() {
        return List.copyOf(items);
    }
}
