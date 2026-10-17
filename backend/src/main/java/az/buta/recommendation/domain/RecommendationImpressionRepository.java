package az.buta.recommendation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RecommendationImpressionRepository extends JpaRepository<RecommendationImpression, UUID> {

    @Query(value = "select offer_id, count(*) from recommendation_impressions where user_id = :userId and served_at >= :since group by offer_id",
            nativeQuery = true)
    List<Object[]> serveCounts(@Param("userId") UUID userId, @Param("since") Instant since);

    @Modifying
    @Query("delete from RecommendationImpression r where r.userId = :userId")
    int deleteByUser(@Param("userId") UUID userId);
}
