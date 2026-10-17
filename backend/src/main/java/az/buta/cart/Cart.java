package az.buta.cart;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class Cart extends AbstractEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("addedAt ASC")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public static Cart open(UUID userId, Instant now) {
        Cart cart = new Cart();
        cart.userId = userId;
        cart.updatedAt = now;
        return cart;
    }

    public Optional<CartItem> findItem(UUID itemId) {
        return items.stream().filter(item -> item.getId().equals(itemId)).findFirst();
    }

    public Optional<CartItem> findByOffer(UUID offerId) {
        return items.stream().filter(item -> item.offerId().equals(offerId)).findFirst();
    }

    public CartItem add(UUID offerId, int quantity, BigDecimal unitPrice, String recommendationRequestId, Instant now) {
        CartItem item = new CartItem(this, offerId, quantity, unitPrice, recommendationRequestId, now);
        items.add(item);
        updatedAt = now;
        return item;
    }

    public void remove(CartItem item, Instant now) {
        items.remove(item);
        updatedAt = now;
    }

    public void clear(Instant now) {
        items.clear();
        updatedAt = now;
    }

    public void touch(Instant now) {
        updatedAt = now;
    }

    public UUID userId() {
        return userId;
    }

    public List<CartItem> items() {
        return List.copyOf(items);
    }
}
