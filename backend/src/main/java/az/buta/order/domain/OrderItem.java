package az.buta.order.domain;

import az.buta.catalog.domain.OfferType;
import az.buta.common.domain.AbstractEntity;
import az.buta.common.i18n.LocalizedText;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", nullable = false)
    private OfferType offerType;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "title_snapshot", nullable = false)
    private LocalizedText titleSnapshot;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_subtotal", nullable = false)
    private BigDecimal lineSubtotal;

    @Column(name = "points_discount_allocated", nullable = false)
    private BigDecimal pointsDiscountAllocated;

    @Column(name = "cash_paid", nullable = false)
    private BigDecimal cashPaid;

    @Column(name = "partner_cost_total", nullable = false)
    private BigDecimal partnerCostTotal;

    @Column(name = "payment_fee_allocated", nullable = false)
    private BigDecimal paymentFeeAllocated;

    @Column(name = "expected_loyalty_cost", nullable = false)
    private BigDecimal expectedLoyaltyCost;

    @Column(name = "contribution_margin", nullable = false)
    private BigDecimal contributionMargin;

    @Column(name = "points_earned", nullable = false)
    private long pointsEarned;

    @Column(name = "refunded_quantity", nullable = false)
    private int refundedQuantity;

    @Column(name = "recommendation_request_id")
    private String recommendationRequestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemComponent> components = new ArrayList<>();

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Redemption> redemptions = new ArrayList<>();

    protected OrderItem() {
    }

    public OrderItem(Order order, UUID offerId, OfferType offerType, UUID categoryId, LocalizedText titleSnapshot, int quantity,
                     BigDecimal unitPrice, BigDecimal lineSubtotal, BigDecimal pointsDiscountAllocated, BigDecimal cashPaid,
                     BigDecimal partnerCostTotal, BigDecimal paymentFeeAllocated, BigDecimal expectedLoyaltyCost,
                     BigDecimal contributionMargin, String recommendationRequestId, Instant createdAt) {
        this.order = order;
        this.offerId = offerId;
        this.offerType = offerType;
        this.categoryId = categoryId;
        this.titleSnapshot = titleSnapshot;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineSubtotal = lineSubtotal;
        this.pointsDiscountAllocated = pointsDiscountAllocated;
        this.cashPaid = cashPaid;
        this.partnerCostTotal = partnerCostTotal;
        this.paymentFeeAllocated = paymentFeeAllocated;
        this.expectedLoyaltyCost = expectedLoyaltyCost;
        this.contributionMargin = contributionMargin;
        this.recommendationRequestId = recommendationRequestId;
        this.createdAt = createdAt;
    }

    public void addComponent(OrderItemComponent component) {
        components.add(component);
    }

    public void addRedemption(Redemption redemption) {
        redemptions.add(redemption);
    }

    public void recordPointsEarned(long points) {
        this.pointsEarned = points;
    }

    public void markRefunded() {
        this.refundedQuantity = quantity;
    }

    public boolean isRefunded() {
        return refundedQuantity >= quantity;
    }

    public Order order() {
        return order;
    }

    public UUID offerId() {
        return offerId;
    }

    public OfferType offerType() {
        return offerType;
    }

    public UUID categoryId() {
        return categoryId;
    }

    public LocalizedText titleSnapshot() {
        return titleSnapshot;
    }

    public int quantity() {
        return quantity;
    }

    public BigDecimal unitPrice() {
        return unitPrice;
    }

    public BigDecimal lineSubtotal() {
        return lineSubtotal;
    }

    public BigDecimal pointsDiscountAllocated() {
        return pointsDiscountAllocated;
    }

    public BigDecimal cashPaid() {
        return cashPaid;
    }

    public BigDecimal partnerCostTotal() {
        return partnerCostTotal;
    }

    public BigDecimal contributionMargin() {
        return contributionMargin;
    }

    public long pointsEarned() {
        return pointsEarned;
    }

    public String recommendationRequestId() {
        return recommendationRequestId;
    }

    public List<OrderItemComponent> components() {
        return List.copyOf(components);
    }

    public List<Redemption> redemptions() {
        return List.copyOf(redemptions);
    }
}
