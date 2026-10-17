package az.buta.catalog.domain;

import az.buta.common.domain.NewAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_inventory")
public class ProductInventory extends NewAwareEntity<UUID> {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private int soldQuantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductInventory() {
    }

    public static ProductInventory create(UUID productId, int totalQuantity, Instant now) {
        ProductInventory inventory = new ProductInventory();
        inventory.productId = productId;
        inventory.totalQuantity = totalQuantity;
        inventory.updatedAt = now;
        return inventory;
    }

    @Override
    public UUID getId() {
        return productId;
    }

    public int available() {
        return Math.max(0, totalQuantity - reservedQuantity - soldQuantity);
    }

    public UUID productId() {
        return productId;
    }

    public int totalQuantity() {
        return totalQuantity;
    }

    public int reservedQuantity() {
        return reservedQuantity;
    }

    public int soldQuantity() {
        return soldQuantity;
    }
}
