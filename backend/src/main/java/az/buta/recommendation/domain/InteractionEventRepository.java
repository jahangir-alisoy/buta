package az.buta.recommendation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InteractionEventRepository extends JpaRepository<InteractionEvent, UUID> {

    @Query("select e from InteractionEvent e where e.userId = :userId and e.occurredAt >= :since")
    List<InteractionEvent> findByUserSince(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query("select count(e) > 0 from InteractionEvent e where e.userId = :userId and e.offerId = :offerId "
            + "and e.eventType = :type and e.occurredAt >= :since")
    boolean existsSince(@Param("userId") UUID userId, @Param("offerId") UUID offerId, @Param("type") InteractionType type,
                        @Param("since") Instant since);

    @Query("select distinct e.offerId from InteractionEvent e where e.userId = :userId and e.offerId is not null "
            + "and e.eventType in :types and e.occurredAt >= :since")
    List<UUID> engagedOfferIds(@Param("userId") UUID userId, @Param("types") Collection<InteractionType> types,
                               @Param("since") Instant since);

    @Modifying
    @Query("delete from InteractionEvent e where e.userId = :userId")
    int deleteByUser(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from InteractionEvent e where e.occurredAt < :cutoff")
    int purgeBefore(@Param("cutoff") Instant cutoff);
}
