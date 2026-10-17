package az.buta.catalog;

import az.buta.catalog.domain.InventoryMovement;
import az.buta.catalog.domain.InventoryMovementRepository;
import az.buta.catalog.domain.MovementType;
import az.buta.catalog.domain.ProductInventory;
import az.buta.catalog.domain.ProductInventoryRepository;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final ProductInventoryRepository inventory;
    private final InventoryMovementRepository movements;
    private final Clock clock;

    public InventoryService(ProductInventoryRepository inventory, InventoryMovementRepository movements, Clock clock) {
        this.inventory = inventory;
        this.movements = movements;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<UUID, Integer> availability(Collection<UUID> productIds) {
        return inventory.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductInventory::productId, ProductInventory::available, (left, right) -> left));
    }

    @Transactional(readOnly = true)
    public Map<UUID, ProductInventory> snapshot(Collection<UUID> productIds) {
        return inventory.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductInventory::productId, Function.identity()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveAll(Map<UUID, Integer> quantities, UUID orderId) {
        Instant now = clock.instant();
        new TreeMap<>(quantities).forEach((productId, quantity) -> {
            if (inventory.reserve(productId, quantity, now) != 1) {
                throw ApiException.withProperties(ErrorCode.INSUFFICIENT_INVENTORY, Map.of("productId", productId.toString()));
            }
            movements.save(new InventoryMovement(productId, MovementType.RESERVE, quantity, orderId, null, null, now));
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseAll(Map<UUID, Integer> quantities, UUID orderId) {
        Instant now = clock.instant();
        new TreeMap<>(quantities).forEach((productId, quantity) -> {
            if (inventory.release(productId, quantity, now) != 1) {
                throw new IllegalStateException("Inventory release mismatch for product " + productId);
            }
            movements.save(new InventoryMovement(productId, MovementType.RELEASE, quantity, orderId, null, null, now));
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void sellAll(Map<UUID, Integer> quantities, UUID orderId) {
        Instant now = clock.instant();
        new TreeMap<>(quantities).forEach((productId, quantity) -> {
            if (inventory.sell(productId, quantity, now) != 1) {
                throw new IllegalStateException("Inventory sell mismatch for product " + productId);
            }
            movements.save(new InventoryMovement(productId, MovementType.SELL, quantity, orderId, null, null, now));
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restock(UUID productId, int quantity, UUID orderId) {
        Instant now = clock.instant();
        if (inventory.restock(productId, quantity, now) == 1) {
            movements.save(new InventoryMovement(productId, MovementType.RESTOCK, quantity, orderId, null, null, now));
        }
    }

    @Transactional
    public void adjust(UUID productId, int delta, String reason, UUID adminId) {
        Instant now = clock.instant();
        if (inventory.adjustTotal(productId, delta, now) != 1) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "delta");
        }
        movements.save(new InventoryMovement(productId, MovementType.ADJUSTMENT, delta, null, reason, adminId, now));
    }

    @Transactional
    public void create(UUID productId, int totalQuantity) {
        inventory.save(ProductInventory.create(productId, totalQuantity, clock.instant()));
    }
}
