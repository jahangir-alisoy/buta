package az.buta.loyalty.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);

    Page<LedgerEntry> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("select coalesce(sum(e.points), 0) from LedgerEntry e where e.userId = :userId")
    long sumPoints(@Param("userId") UUID userId);
}
