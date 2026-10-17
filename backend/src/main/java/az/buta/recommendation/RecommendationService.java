package az.buta.recommendation;

import az.buta.catalog.OfferCatalog;
import az.buta.catalog.OfferPopularityService;
import az.buta.catalog.OfferSnapshot;
import az.buta.catalog.OfferViewAssembler;
import az.buta.catalog.Viewer;
import az.buta.catalog.domain.Category;
import az.buta.catalog.domain.CategoryRepository;
import az.buta.loyalty.LoyaltyQueryService;
import az.buta.loyalty.domain.TaskType;
import az.buta.loyalty.dto.LoyaltySummaryDto;
import az.buta.loyalty.dto.TaskDto;
import az.buta.loyalty.task.PurchaseFact;
import az.buta.loyalty.task.UserFactsLoader;
import az.buta.membership.MembershipFeature;
import az.buta.pricing.BusinessSettingsService;
import az.buta.pricing.RecommendationSettings;
import az.buta.recommendation.domain.InterestProfile;
import az.buta.recommendation.domain.InteractionEventRepository;
import az.buta.recommendation.domain.InteractionType;
import az.buta.recommendation.domain.RecommendationImpression;
import az.buta.recommendation.domain.RecommendationImpressionRepository;
import az.buta.recommendation.dto.RecommendationsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    public static final String ALGORITHM_VERSION = "rules-v1";

    private final OfferCatalog catalog;
    private final OfferViewAssembler assembler;
    private final OfferPopularityService popularity;
    private final InterestProfileService profiles;
    private final InteractionService interactions;
    private final InteractionEventRepository events;
    private final RecommendationImpressionRepository impressions;
    private final RecommendationScorer scorer;
    private final RecommendationReRanker reRanker;
    private final LoyaltyQueryService loyalty;
    private final UserFactsLoader factsLoader;
    private final CategoryRepository categories;
    private final BusinessSettingsService settings;
    private final Clock clock;

    public RecommendationService(OfferCatalog catalog, OfferViewAssembler assembler, OfferPopularityService popularity,
                                 InterestProfileService profiles, InteractionService interactions, InteractionEventRepository events,
                                 RecommendationImpressionRepository impressions, RecommendationScorer scorer, RecommendationReRanker reRanker,
                                 LoyaltyQueryService loyalty, UserFactsLoader factsLoader, CategoryRepository categories,
                                 BusinessSettingsService settings, Clock clock) {
        this.catalog = catalog;
        this.assembler = assembler;
        this.popularity = popularity;
        this.profiles = profiles;
        this.interactions = interactions;
        this.events = events;
        this.impressions = impressions;
        this.scorer = scorer;
        this.reRanker = reRanker;
        this.loyalty = loyalty;
        this.factsLoader = factsLoader;
        this.categories = categories;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional
    public RecommendationsDto recommend(Viewer viewer, Placement placement, UUID contextOfferId, int limit) {
        Instant now = clock.instant();
        RecommendationSettings config = settings.recommendation();
        String variant = variantFor(viewer.userId(), config);
        RecommendationSettings.Weights weights = config.variants().stream().filter(candidate -> candidate.name().equals(variant))
                .map(RecommendationSettings.Variant::weights).filter(java.util.Objects::nonNull).findFirst().orElse(config.weights());
        List<OfferSnapshot> eligible = catalog.onSale().stream()
                .filter(snapshot -> OfferViewAssembler.blockReason(snapshot, viewer) == null)
                .filter(snapshot -> !snapshot.id().equals(contextOfferId))
                .toList();
        boolean personalized = placement != Placement.OFFER_DETAIL && viewer.isAuthenticated()
                && viewer.has(MembershipFeature.PERSONALIZED_RECOMMENDATIONS) && interactions.personalizationEnabled(viewer.userId());
        List<ScoredOffer> ranked;
        if (placement == Placement.OFFER_DETAIL && contextOfferId != null) {
            ranked = similar(contextOfferId, eligible, limit);
        } else if (personalized) {
            ranked = personalizedRanking(viewer, eligible, limit, config, weights, now);
        } else {
            ranked = reRanker.rerank(List.of(), List.of(), popular(eligible), limit, config);
        }
        String requestId = UUID.randomUUID().toString().replace("-", "");
        Map<UUID, String> categoryNames = categories.findAll().stream()
                .collect(Collectors.toMap(Category::getId, category -> category.name().in(viewer.locale())));
        List<RecommendationsDto.Item> items = new ArrayList<>();
        int position = 0;
        for (ScoredOffer scored : ranked) {
            impressions.save(new RecommendationImpression(requestId, viewer.userId(), placement.name(), ALGORITHM_VERSION, variant,
                    scored.offer().id(), position++, BigDecimal.valueOf(scored.score()).setScale(2, java.math.RoundingMode.HALF_UP),
                    scored.components(), scored.reason() == null ? null : scored.reason().code(), now));
            items.add(new RecommendationsDto.Item(assembler.card(scored.offer(), viewer),
                    scored.reason() == null ? null : scored.reason().code(), localize(scored, categoryNames), scored.exploration()));
        }
        return new RecommendationsDto(requestId, placement.name(), personalized, ALGORITHM_VERSION, variant, items);
    }

    private List<ScoredOffer> personalizedRanking(Viewer viewer, List<OfferSnapshot> eligible, int limit, RecommendationSettings config,
                                                  RecommendationSettings.Weights weights, Instant now) {
        InterestProfile profile = profiles.profileOf(viewer.userId());
        if (profile.signals() == 0) {
            return reRanker.rerank(List.of(), List.of(), popular(eligible), limit, config);
        }
        LoyaltyContext loyaltyContext = loyaltyContext(viewer, now);
        Map<UUID, Long> serves = serveCounts(viewer.userId(), now, config.frequencyCap().windowDays());
        Set<UUID> engaged = new HashSet<>(events.engagedOfferIds(viewer.userId(),
                Set.of(InteractionType.DETAIL_VIEW, InteractionType.CART_ADD, InteractionType.RECOMMENDATION_CLICK),
                now.minus(Duration.ofDays(config.frequencyCap().windowDays()))));
        List<ScoredOffer> scored = eligible.stream()
                .map(offer -> scorer.score(offer, profile, loyaltyContext, weights, config.minRelevance(),
                        engaged.contains(offer.id()) ? 0 : serves.getOrDefault(offer.id(), 0L), config.frequencyCap().maxServes()))
                .sorted(Comparator.comparingDouble(ScoredOffer::score).reversed().thenComparing(offer -> offer.offer().id().toString()))
                .toList();
        List<ScoredOffer> relevant = scored.stream().filter(offer -> offer.component(RecommendationScorer.INTEREST) >= config.minRelevance()).toList();
        List<ScoredOffer> exploration = scored.stream()
                .filter(offer -> offer.component(RecommendationScorer.INTEREST) < config.minRelevance()
                        && offer.component(RecommendationScorer.VALUE) >= 50 && offer.component(RecommendationScorer.DISCOVERY) > 0)
                .sorted(Comparator.comparingDouble((ScoredOffer offer) -> offer.component(RecommendationScorer.VALUE)
                        + offer.component(RecommendationScorer.BUSINESS)).reversed())
                .toList();
        return reRanker.rerank(relevant, exploration, popular(eligible), limit, config);
    }

    private List<ScoredOffer> popular(List<OfferSnapshot> eligible) {
        Map<UUID, Long> counts = popularity.fulfilledOrderCounts();
        return eligible.stream()
                .sorted(Comparator.comparing((OfferSnapshot offer) -> counts.getOrDefault(offer.id(), 0L)).reversed()
                        .thenComparing(OfferSnapshot::featured, Comparator.reverseOrder())
                        .thenComparing(OfferSnapshot::businessPriority, Comparator.reverseOrder())
                        .thenComparing((OfferSnapshot offer) -> offer.pricing().savingsPercent(), Comparator.reverseOrder())
                        .thenComparing(offer -> offer.id().toString()))
                .map(offer -> new ScoredOffer(offer, Map.of(), 0, popularReason(offer, counts.getOrDefault(offer.id(), 0L)), false))
                .toList();
    }

    private List<ScoredOffer> similar(UUID offerId, List<OfferSnapshot> eligible, int limit) {
        OfferSnapshot context = catalog.byIds(List.of(offerId)).get(offerId);
        if (context == null) {
            return List.of();
        }
        Set<UUID> contextCategories = new HashSet<>(context.categoryIds());
        return eligible.stream()
                .map(offer -> {
                    long overlap = offer.categoryIds().stream().filter(contextCategories::contains).count();
                    double score = overlap * 50 + offer.pricing().savingsPercent().doubleValue();
                    return new ScoredOffer(offer, Map.of("overlap", (double) overlap), score,
                            overlap > 0 ? new ScoredOffer.Reason("REASON_SIMILAR", Map.of()) : popularReason(offer, 0), false);
                })
                .sorted(Comparator.comparingDouble(ScoredOffer::score).reversed().thenComparing(offer -> offer.offer().id().toString()))
                .limit(limit)
                .toList();
    }

    private LoyaltyContext loyaltyContext(Viewer viewer, Instant now) {
        if (!viewer.has(MembershipFeature.LOYALTY_POINTS)) {
            return LoyaltyContext.none();
        }
        LoyaltySummaryDto summary = loyalty.summary(viewer.userId(), viewer.locale());
        List<TaskDto> tasks = loyalty.tasks(viewer.userId(), viewer.locale());
        List<PurchaseFact> purchases = factsLoader.load(viewer.userId()).purchases();
        List<LoyaltyContext.OpenTask> open = tasks.stream()
                .filter(task -> !"COMPLETED".equals(task.status()))
                .map(task -> new LoyaltyContext.OpenTask(task.code(), task.title(), TaskType.valueOf(task.type()), task.progress(), task.target(),
                        task.rewardPoints(), partnersInPeriod(purchases, task.periodKey())))
                .toList();
        return new LoyaltyContext(true, summary.nextTier() == null ? 0 : summary.nextTier().pointsNeeded(),
                summary.nextTier() == null ? null : summary.nextTier().name(), viewer.tierMultiplier(),
                settings.loyalty().pointsPerAzn(), open);
    }

    private static Set<UUID> partnersInPeriod(List<PurchaseFact> purchases, String periodKey) {
        return purchases.stream()
                .filter(purchase -> "ONE_TIME".equals(periodKey) || purchase.purchasedAt().toString().startsWith(periodKey))
                .flatMap(purchase -> purchase.partnerIds().stream())
                .collect(Collectors.toSet());
    }

    private Map<UUID, Long> serveCounts(UUID userId, Instant now, int windowDays) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : impressions.serveCounts(userId, now.minus(Duration.ofDays(windowDays)))) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private static ScoredOffer.Reason popularReason(OfferSnapshot offer, long count) {
        if (count > 0) {
            return new ScoredOffer.Reason("REASON_POPULAR", Map.of());
        }
        if (offer.pricing().savingsPercent().compareTo(BigDecimal.valueOf(20)) >= 0) {
            return new ScoredOffer.Reason("REASON_GREAT_VALUE", Map.of("savingsPercent",
                    offer.pricing().savingsPercent().stripTrailingZeros().toPlainString()));
        }
        return null;
    }

    private static Map<String, String> localize(ScoredOffer scored, Map<UUID, String> categoryNames) {
        if (scored.reason() == null) {
            return Map.of();
        }
        Map<String, String> params = new LinkedHashMap<>(scored.reason().params());
        if (params.containsKey("categoryId")) {
            params.put("category", categoryNames.getOrDefault(UUID.fromString(params.get("categoryId")), ""));
        } else if ("REASON_TRY_NEW".equals(scored.reason().code())) {
            params.put("category", categoryNames.getOrDefault(scored.offer().primaryCategory().id(), ""));
        }
        return params;
    }

    static String variantFor(UUID userId, RecommendationSettings config) {
        if (config.variants().size() == 1 || userId == null) {
            return config.variants().getFirst().name();
        }
        int bucket = Math.floorMod((userId + ":recommendation").hashCode(), 100);
        int cumulative = 0;
        for (RecommendationSettings.Variant variant : config.variants()) {
            cumulative += variant.allocationPercent();
            if (bucket < cumulative) {
                return variant.name();
            }
        }
        return config.variants().getLast().name();
    }
}
