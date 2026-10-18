package az.buta.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MembershipPurchaseRepository extends JpaRepository<MembershipPurchase, UUID> {

    boolean existsByProviderReference(String providerReference);
}
