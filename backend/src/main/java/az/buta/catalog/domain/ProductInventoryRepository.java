package az.buta.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, UUID> {

    @Modifying(flushAutomatically = true)
    @Query(value = "update product_inventory set reserved_quantity = reserved_quantity + :quantity, updated_at = :now "
            + "where product_id = :productId and total_quantity - reserved_quantity - sold_quantity >= :quantity", nativeQuery = true)
    int reserve(@Param("productId") UUID productId, @Param("quantity") int quantity, @Param("now") Instant now);

    @Modifying(flushAutomatically = true)
    @Query(value = "update product_inventory set reserved_quantity = reserved_quantity - :quantity, updated_at = :now "
            + "where product_id = :productId and reserved_quantity >= :quantity", nativeQuery = true)
    int release(@Param("productId") UUID productId, @Param("quantity") int quantity, @Param("now") Instant now);

    @Modifying(flushAutomatically = true)
    @Query(value = "update product_inventory set reserved_quantity = reserved_quantity - :quantity, "
            + "sold_quantity = sold_quantity + :quantity, updated_at = :now "
            + "where product_id = :productId and reserved_quantity >= :quantity", nativeQuery = true)
    int sell(@Param("productId") UUID productId, @Param("quantity") int quantity, @Param("now") Instant now);

    @Modifying(flushAutomatically = true)
    @Query(value = "update product_inventory set sold_quantity = sold_quantity - :quantity, updated_at = :now "
            + "where product_id = :productId and sold_quantity >= :quantity", nativeQuery = true)
    int restock(@Param("productId") UUID productId, @Param("quantity") int quantity, @Param("now") Instant now);

    @Modifying(flushAutomatically = true)
    @Query(value = "update product_inventory set total_quantity = total_quantity + :delta, updated_at = :now "
            + "where product_id = :productId and total_quantity + :delta >= reserved_quantity + sold_quantity "
            + "and total_quantity + :delta >= 0", nativeQuery = true)
    int adjustTotal(@Param("productId") UUID productId, @Param("delta") int delta, @Param("now") Instant now);
}
