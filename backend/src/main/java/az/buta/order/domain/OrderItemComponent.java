package az.buta.order.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_item_components")
public class OrderItemComponent extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "allocated_price", nullable = false)
    private BigDecimal allocatedPrice;

    @Column(name = "partner_cost", nullable = false)
    private BigDecimal partnerCost;

    protected OrderItemComponent() {
    }

    public OrderItemComponent(OrderItem orderItem, UUID productId, UUID partnerId, int quantity, BigDecimal allocatedPrice, BigDecimal partnerCost) {
        this.orderItem = orderItem;
        this.productId = productId;
        this.partnerId = partnerId;
        this.quantity = quantity;
        this.allocatedPrice = allocatedPrice;
        this.partnerCost = partnerCost;
    }

    public UUID productId() {
        return productId;
    }

    public UUID partnerId() {
        return partnerId;
    }

    public int quantity() {
        return quantity;
    }
}
