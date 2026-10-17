package az.buta.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTier, String> {

    List<LoyaltyTier> findAllByOrderByThresholdPointsAsc();
}
