package az.buta.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    Optional<Referral> findByRefereeUserId(UUID refereeUserId);

    boolean existsByRefereeUserId(UUID refereeUserId);

    Optional<Referral> findByQualifyingOrderId(UUID orderId);

    List<Referral> findByReferrerUserIdOrderByCreatedAtDesc(UUID referrerUserId);

    List<Referral> findByStatusOrderByCreatedAtDesc(ReferralStatus status);

    @Query("select count(r) from Referral r where r.referrerUserId = :referrerId and r.status = az.buta.loyalty.domain.ReferralStatus.QUALIFIED "
            + "and r.qualifiedAt >= :since")
    long countQualifiedSince(@Param("referrerId") UUID referrerId, @Param("since") Instant since);

    @Query("select r.qualifiedAt from Referral r where r.referrerUserId = :referrerId and r.status = az.buta.loyalty.domain.ReferralStatus.QUALIFIED")
    List<Instant> qualifiedInstants(@Param("referrerId") UUID referrerId);
}
