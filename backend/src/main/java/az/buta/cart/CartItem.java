package az.buta.cart;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItem extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_at_add", nullable = false)
    private BigDecimal unitPriceAtAdd;

    @Column(name = "recommendation_request_id")
    private String recommendationRequestId;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected CartItem() {
    }

    CartItem(Cart cart, UUID offerId, int quantity, BigDecimal unitPriceAtAdd, String recommendationRequestId, Instant addedAt) {
        this.cart = cart;
        this.offerId = offerId;
        this.quantity = quantity;
        this.unitPriceAtAdd = unitPriceAtAdd;
        this.recommendationRequestId = recommendationRequestId;
        this.addedAt = addedAt;
    }

    public void changeQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void refreshPrice(BigDecimal unitPrice) {
        this.unitPriceAtAdd = unitPrice;
    }

    public UUID offerId() {
        return offerId;
    }

    public int quantity() {
        return quantity;
    }

    public BigDecimal unitPriceAtAdd() {
        return unitPriceAtAdd;
    }

    public String recommendationRequestId() {
        return recommendationRequestId;
    }
}
