package az.buta.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update UserSession s set s.revokedAt = :now, s.revokeReason = :reason where s.userId = :userId and s.revokedAt is null")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now, @Param("reason") String reason);

    @Modifying
    @Query("update UserSession s set s.revokedAt = :now, s.revokeReason = :reason where s.userId = :userId and s.revokedAt is null and s.id <> :keepId")
    int revokeOthers(@Param("userId") UUID userId, @Param("keepId") UUID keepId, @Param("now") Instant now, @Param("reason") String reason);

    @Modifying
    @Query("delete from UserSession s where s.absoluteExpiresAt < :cutoff or (s.revokedAt is not null and s.revokedAt < :cutoff)")
    int purgeBefore(@Param("cutoff") Instant cutoff);
}
