package az.buta.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RedemptionRepository extends JpaRepository<Redemption, UUID> {

    Optional<Redemption> findByCode(String code);

    boolean existsByCode(String code);

    @Query("select r from Redemption r where r.status = az.buta.order.domain.RedemptionStatus.ISSUED and r.validUntil < :now")
    List<Redemption> findDueForExpiry(@Param("now") Instant now);
}
