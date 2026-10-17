package az.buta.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID>, JpaSpecificationExecutor<Offer> {

    Optional<Offer> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("select o from Offer o where o.status = az.buta.catalog.domain.OfferStatus.PUBLISHED "
            + "and o.saleStartsAt <= :now and o.saleEndsAt > :now")
    List<Offer> findOnSale(@Param("now") Instant now);

    @Query("select distinct o from Offer o join o.components c where c.product.id in :productIds")
    List<Offer> findContainingProducts(@Param("productIds") Collection<UUID> productIds);

    @Query("select distinct o from Offer o join o.components c where c.product.partner.id = :partnerId "
            + "and o.status = az.buta.catalog.domain.OfferStatus.PUBLISHED and o.saleStartsAt <= :now and o.saleEndsAt > :now")
    List<Offer> findOnSaleByPartner(@Param("partnerId") UUID partnerId, @Param("now") Instant now);
}
