package az.buta.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    List<Partner> findByStatusOrderByNameAsc(ActiveStatus status);

    List<Partner> findAllByOrderByNameAsc();

    Optional<Partner> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
