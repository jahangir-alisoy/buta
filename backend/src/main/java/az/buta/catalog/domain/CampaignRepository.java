package az.buta.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    @Query("select distinct c from Campaign c left join fetch c.offers where c.status = az.buta.catalog.domain.ActiveStatus.ACTIVE "
            + "and c.startsAt <= :now and c.endsAt > :now")
    List<Campaign> findRunning(@Param("now") Instant now);

    @Query("select distinct c from Campaign c left join fetch c.offers order by c.startsAt desc")
    List<Campaign> findAllWithOffers();

    boolean existsBySlug(String slug);
}
