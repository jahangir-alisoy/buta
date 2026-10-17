package az.buta.loyalty.task;

import az.buta.loyalty.domain.ReferralRepository;
import az.buta.pricing.AwardTrigger;
import az.buta.pricing.BusinessSettingsService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
public class UserFactsLoader {

    private final EntityManager entityManager;
    private final ReferralRepository referrals;
    private final BusinessSettingsService settings;

    public UserFactsLoader(EntityManager entityManager, ReferralRepository referrals, BusinessSettingsService settings) {
        this.entityManager = entityManager;
        this.referrals = referrals;
        this.settings = settings;
    }

    public UserFacts load(UUID userId) {
        return new UserFacts(purchases(userId), attendances(userId),
                referrals.qualifiedInstants(userId).stream().filter(Objects::nonNull).toList());
    }

    private List<PurchaseFact> purchases(UUID userId) {
        boolean completedOnly = settings.loyalty().awardTrigger() == AwardTrigger.COMPLETED;
        String statuses = completedOnly ? "('COMPLETED')" : "('FULFILLED', 'COMPLETED', 'PARTIALLY_REFUNDED')";
        String instantColumn = completedOnly ? "o.completed_at" : "o.fulfilled_at";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select o.id, i.id, i.offer_type, i.category_id, c.partner_id, " + instantColumn + ", o.total_amount "
                                + "from orders o join order_items i on i.order_id = o.id "
                                + "join order_item_components c on c.order_item_id = i.id "
                                + "where o.user_id = :userId and o.status in " + statuses + " and i.refunded_quantity = 0")
                .setParameter("userId", userId)
                .getResultList();
        Map<UUID, PurchaseFact> facts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            UUID itemId = (UUID) row[1];
            PurchaseFact existing = facts.get(itemId);
            Set<UUID> partners = existing == null ? new HashSet<>() : new HashSet<>(existing.partnerIds());
            partners.add((UUID) row[4]);
            facts.put(itemId, new PurchaseFact((UUID) row[0], itemId, "BUNDLE".equals(row[2]), (UUID) row[3], Set.copyOf(partners),
                    instant(row[5]), (BigDecimal) row[6]));
        }
        return new ArrayList<>(facts.values());
    }

    private List<Instant> attendances(UUID userId) {
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery(
                        "select r.redeemed_at from redemptions r join order_items i on i.id = r.order_item_id "
                                + "join orders o on o.id = i.order_id join partner_products p on p.id = r.product_id "
                                + "where o.user_id = :userId and r.status = 'REDEEMED' and p.product_type = 'EVENT_TICKET'")
                .setParameter("userId", userId)
                .getResultList();
        return rows.stream().map(UserFactsLoader::instant).filter(Objects::nonNull).toList();
    }

    private static Instant instant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offset) {
            return offset.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalStateException("Unsupported timestamp type " + value.getClass());
    }
}
