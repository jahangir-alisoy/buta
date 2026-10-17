package az.buta.recommendation;

import az.buta.catalog.OfferCatalog;
import az.buta.catalog.OfferSnapshot;
import az.buta.favorite.Favorite;
import az.buta.favorite.FavoriteRepository;
import az.buta.pricing.BusinessSettingsService;
import az.buta.pricing.RecommendationSettings;
import az.buta.recommendation.domain.InteractionEvent;
import az.buta.recommendation.domain.InteractionEventRepository;
import az.buta.recommendation.domain.InterestProfile;
import az.buta.recommendation.domain.UserInterestProfile;
import az.buta.recommendation.domain.UserInterestProfileRepository;
import az.buta.user.domain.UserPreferences;
import az.buta.user.domain.UserPreferencesRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class InterestProfileService {

    private static final Duration FRESHNESS = Duration.ofMinutes(10);

    private final UserInterestProfileRepository profiles;
    private final InteractionEventRepository events;
    private final FavoriteRepository favorites;
    private final UserPreferencesRepository preferences;
    private final OfferCatalog catalog;
    private final BusinessSettingsService settings;
    private final EntityManager entityManager;
    private final Clock clock;

    public InterestProfileService(UserInterestProfileRepository profiles, InteractionEventRepository events, FavoriteRepository favorites,
                                  UserPreferencesRepository preferences, OfferCatalog catalog, BusinessSettingsService settings,
                                  EntityManager entityManager, Clock clock) {
        this.profiles = profiles;
        this.events = events;
        this.favorites = favorites;
        this.preferences = preferences;
        this.catalog = catalog;
        this.settings = settings;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public InterestProfile profileOf(UUID userId) {
        Instant now = clock.instant();
        UserInterestProfile stored = profiles.findById(userId).orElse(null);
        if (stored != null && stored.computedAt() != null && stored.computedAt().plus(FRESHNESS).isAfter(now)) {
            return stored.toProfile();
        }
        InterestProfile computed = compute(userId, now);
        UserInterestProfile target = stored == null ? new UserInterestProfile(userId) : stored;
        target.replace(computed, now);
        profiles.save(target);
        return computed;
    }

    @Transactional
    public void invalidate(UUID userId) {
        profiles.findById(userId).ifPresent(profiles::delete);
    }

    @Transactional
    public void reset(UUID userId) {
        events.deleteByUser(userId);
        profiles.findById(userId).ifPresent(profiles::delete);
    }

    private InterestProfile compute(UUID userId, Instant now) {
        RecommendationSettings config = settings.recommendation();
        RecommendationSettings.SignalWeights weights = config.signalWeights();
        double halfLife = config.decayHalfLifeDays();
        Map<UUID, Double> categories = new HashMap<>();
        Map<UUID, Double> partners = new HashMap<>();
        Set<UUID> purchasedCategories = new HashSet<>();
        List<BigDecimal> paidPrices = new ArrayList<>();
        int signals = 0;
        int sensitive = 0;

        List<Purchase> purchases = purchases(userId);
        Set<UUID> offerIds = new HashSet<>();
        purchases.forEach(purchase -> offerIds.add(purchase.offerId()));
        List<Favorite> favoriteList = favorites.findByUser(userId);
        favoriteList.forEach(favorite -> offerIds.add(favorite.offerId()));
        Map<UUID, OfferSnapshot> snapshots = catalog.byIds(offerIds);

        for (Purchase purchase : purchases) {
            double weight = weights.purchase() * decay(purchase.at(), now, halfLife);
            categories.merge(purchase.categoryId(), weight, Double::sum);
            purchase.partnerIds().forEach(partner -> partners.merge(partner, weight, Double::sum));
            purchasedCategories.add(purchase.categoryId());
            paidPrices.add(purchase.unitPrice());
            OfferSnapshot snapshot = snapshots.get(purchase.offerId());
            if (snapshot != null && snapshot.pricing().savingsPercent().compareTo(BigDecimal.valueOf(25)) >= 0) {
                sensitive++;
            }
            signals++;
        }
        for (Favorite favorite : favoriteList) {
            OfferSnapshot snapshot = snapshots.get(favorite.offerId());
            if (snapshot == null) {
                continue;
            }
            double weight = weights.favorite() * decay(favorite.createdAt(), now, halfLife);
            snapshot.categoryIds().forEach(category -> categories.merge(category, weight, Double::sum));
            snapshot.partners().forEach(partner -> partners.merge(partner.id(), weight * 0.5, Double::sum));
            signals++;
        }
        for (InteractionEvent event : events.findByUserSince(userId, now.minus(Duration.ofDays(config.eventRetentionDays())))) {
            double base = switch (event.eventType()) {
                case CART_ADD -> weights.cartAdd();
                case DETAIL_VIEW, RECOMMENDATION_CLICK -> weights.detailView();
                case SEARCH, CATEGORY_VIEW -> weights.search();
                default -> 0.0;
            };
            if (base > 0 && event.categoryId() != null) {
                categories.merge(event.categoryId(), base * decay(event.occurredAt(), now, halfLife), Double::sum);
                signals++;
            }
        }
        UserPreferences prefs = preferences.findById(userId).orElse(null);
        if (prefs != null) {
            for (UUID category : prefs.preferredCategoryIds()) {
                categories.merge(category, weights.explicitPreference(), Double::sum);
                signals++;
            }
        }
        BigDecimal low = null;
        BigDecimal high = null;
        if (paidPrices.size() >= 3) {
            List<BigDecimal> sorted = paidPrices.stream().sorted().toList();
            low = sorted.get((int) Math.floor((sorted.size() - 1) * 0.25));
            high = sorted.get((int) Math.ceil((sorted.size() - 1) * 0.75));
        } else if (prefs != null && prefs.budgetMin() != null && prefs.budgetMax() != null) {
            low = prefs.budgetMin();
            high = prefs.budgetMax();
        } else if (!paidPrices.isEmpty()) {
            BigDecimal average = paidPrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(paidPrices.size()), 2, RoundingMode.HALF_UP);
            low = average.multiply(BigDecimal.valueOf(0.7)).setScale(2, RoundingMode.HALF_UP);
            high = average.multiply(BigDecimal.valueOf(1.3)).setScale(2, RoundingMode.HALF_UP);
        }
        double sensitivity = purchases.isEmpty() ? 0 : sensitive / (double) purchases.size();
        return new InterestProfile(normalize(categories), normalize(partners), purchasedCategories, low, high, sensitivity, signals);
    }

    private List<Purchase> purchases(UUID userId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select i.id, i.offer_id, i.category_id, i.unit_price, o.fulfilled_at, c.partner_id from orders o "
                                + "join order_items i on i.order_id = o.id join order_item_components c on c.order_item_id = i.id "
                                + "where o.user_id = :userId and o.status in ('FULFILLED', 'COMPLETED', 'PARTIALLY_REFUNDED') "
                                + "and i.refunded_quantity = 0")
                .setParameter("userId", userId)
                .getResultList();
        Map<UUID, Purchase> byItem = new HashMap<>();
        for (Object[] row : rows) {
            UUID itemId = (UUID) row[0];
            Purchase existing = byItem.get(itemId);
            Set<UUID> partnerIds = existing == null ? new HashSet<>() : new HashSet<>(existing.partnerIds());
            partnerIds.add((UUID) row[5]);
            byItem.put(itemId, new Purchase((UUID) row[1], (UUID) row[2], (BigDecimal) row[3], instant(row[4]), partnerIds));
        }
        return new ArrayList<>(byItem.values());
    }

    private static Map<UUID, Double> normalize(Map<UUID, Double> raw) {
        double max = raw.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        Map<UUID, Double> normalized = new HashMap<>();
        if (max <= 0) {
            return normalized;
        }
        raw.forEach((key, value) -> normalized.put(key, value / max * 100.0));
        return normalized;
    }

    private static double decay(Instant at, Instant now, double halfLifeDays) {
        if (at == null) {
            return 1.0;
        }
        double ageDays = Math.max(0, Duration.between(at, now).toHours() / 24.0);
        return Math.pow(0.5, ageDays / halfLifeDays);
    }

    private static Instant instant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offset) {
            return offset.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        return null;
    }

    private record Purchase(UUID offerId, UUID categoryId, BigDecimal unitPrice, Instant at, Set<UUID> partnerIds) {
    }
}
