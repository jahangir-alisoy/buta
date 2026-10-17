package az.buta.catalog;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OfferPopularityService {

    private final EntityManager entityManager;
    private final Clock clock;
    private final Cache<String, Map<UUID, Long>> cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(60)).build();

    public OfferPopularityService(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    public Map<UUID, Long> fulfilledOrderCounts() {
        return cache.get("30d", ignored -> load());
    }

    @Transactional(readOnly = true)
    public Map<UUID, Long> load() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select i.offer_id, count(distinct o.id) from order_items i join orders o on o.id = i.order_id "
                                + "where o.status in ('FULFILLED', 'COMPLETED', 'PARTIALLY_REFUNDED') and o.fulfilled_at >= :since "
                                + "and i.refunded_quantity < i.quantity group by i.offer_id")
                .setParameter("since", clock.instant().minus(Duration.ofDays(30)))
                .getResultList();
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    public void evict() {
        cache.invalidateAll();
    }
}
