package az.buta.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PartnerProductRepository extends JpaRepository<PartnerProduct, UUID> {

    @Query("select p from PartnerProduct p join fetch p.partner join fetch p.category order by p.createdAt desc")
    List<PartnerProduct> findAllWithRelations();

    List<PartnerProduct> findByPartnerId(UUID partnerId);
}
