package az.buta.loyalty.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LoyaltyAccount a where a.userId = :userId")
    Optional<LoyaltyAccount> findForUpdate(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "insert into loyalty_accounts (user_id, tier_code, updated_at) values (:userId, :tierCode, :now) "
            + "on conflict (user_id) do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("tierCode") String tierCode, @Param("now") Instant now);
}
