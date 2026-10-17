package az.buta.recommendation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserInterestProfileRepository extends JpaRepository<UserInterestProfile, UUID> {
}
