package az.buta.order.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByUserIdAndCheckoutIdempotencyKey(UUID userId, String key);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findForUpdate(@Param("id") UUID id);

    @Query(value = "select id from orders where status = 'PENDING_PAYMENT' and expires_at < :now order by expires_at limit :limit "
            + "for update skip locked", nativeQuery = true)
    List<UUID> lockExpiredPending(@Param("now") Instant now, @Param("limit") int limit);

    @Query("select o from Order o where o.userId = :userId and o.status in :statuses order by o.fulfilledAt asc")
    List<Order> findByUserAndStatuses(@Param("userId") UUID userId, @Param("statuses") Collection<OrderStatus> statuses);

    @Query("select o from Order o where (:status is null or o.status = :status) and (:query is null or o.orderNumber like :query) "
            + "order by o.createdAt desc")
    Page<Order> search(@Param("status") OrderStatus status, @Param("query") String query, Pageable pageable);

    @Query(value = "select nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumber();
}
