package az.buta.catalog.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement extends AbstractEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "order_id")
    private UUID orderId;

    private String reason;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryMovement() {
    }

    public InventoryMovement(UUID productId, MovementType movementType, int quantity, UUID orderId, String reason,
                             UUID actorUserId, Instant createdAt) {
        this.productId = productId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.orderId = orderId;
        this.reason = reason;
        this.actorUserId = actorUserId;
        this.createdAt = createdAt;
    }
}
