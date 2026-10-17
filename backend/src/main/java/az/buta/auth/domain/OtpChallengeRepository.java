package az.buta.auth.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findFirstByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OtpChallenge c where c.email = :email and c.purpose = :purpose and c.consumedAt is null "
            + "and c.lockedAt is null and c.supersededAt is null and c.expiresAt > :now order by c.createdAt desc")
    List<OtpChallenge> findOpenForUpdate(@Param("email") String email, @Param("purpose") OtpPurpose purpose, @Param("now") Instant now);

    Optional<OtpChallenge> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, OtpPurpose purpose);

    @Query("select count(c) from OtpChallenge c where c.email = :email and c.createdAt > :since")
    long countRequestsSince(@Param("email") String email, @Param("since") Instant since);

    @Query("select max(c.lockedAt) from OtpChallenge c where c.email = :email and c.lockedAt > :since")
    Optional<Instant> latestLockSince(@Param("email") String email, @Param("since") Instant since);

    @Query("select count(c) from OtpChallenge c where c.email = :email and c.lockedAt > :since")
    long countLocksSince(@Param("email") String email, @Param("since") Instant since);

    @Modifying
    @Query("delete from OtpChallenge c where c.createdAt < :cutoff")
    int purgeBefore(@Param("cutoff") Instant cutoff);
}
