package az.buta.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    @Query("select m from Membership m where m.planCode = az.buta.membership.PlanCode.PLUS and m.endsAt is not null and m.endsAt < :now")
    List<Membership> findExpiredPlus(@Param("now") Instant now);

    @Query(value = "select coalesce(sum(o.total_amount - coalesce(p.refunded_amount, 0)), 0) from orders o "
            + "left join payments p on p.order_id = o.id "
            + "where o.user_id = :userId and o.status in ('FULFILLED', 'COMPLETED', 'PARTIALLY_REFUNDED') "
            + "and o.fulfilled_at >= :since", nativeQuery = true)
    BigDecimal qualifyingSpendSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
