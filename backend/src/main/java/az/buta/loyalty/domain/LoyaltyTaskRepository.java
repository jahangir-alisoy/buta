package az.buta.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoyaltyTaskRepository extends JpaRepository<LoyaltyTask, UUID> {

    List<LoyaltyTask> findByActiveTrue();

    List<LoyaltyTask> findAllByOrderByCreatedAtAsc();

    boolean existsByCode(String code);
}
