package az.buta.recommendation;

import az.buta.catalog.OfferSnapshot;
import az.buta.catalog.domain.OfferType;
import az.buta.catalog.domain.OfferVisibility;
import az.buta.catalog.domain.ProductType;
import az.buta.common.i18n.LocalizedText;
import az.buta.loyalty.domain.TaskType;
import az.buta.pricing.PriceBreakdown;
import az.buta.pricing.RecommendationSettings;
import az.buta.recommendation.domain.InterestProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationScorerTest {

    private static final UUID CINEMA = UUID.randomUUID();
    private static final UUID THEATRE = UUID.randomUUID();
    private static final RecommendationSettings.Weights WEIGHTS = new RecommendationSettings.Weights(30, 20, 15, 15, 10, 10);
    private static final RecommendationSettings SETTINGS = new RecommendationSettings(WEIGHTS,
            new RecommendationSettings.SignalWeights(5, 4, 3, 2, 0.5, 0.5), 20, 15, 60, 180,
            new RecommendationSettings.FrequencyCap(3, 7), new RecommendationSettings.Diversity(2, 3),
            List.of(new RecommendationSettings.Variant("control", 100, null)));

    private final RecommendationScorer scorer = new RecommendationScorer();
    private final RecommendationReRanker reRanker = new RecommendationReRanker();

    @Test
    void interestingOffersOutrankUninterestingOnes() {
        InterestProfile profile = profile(Map.of(CINEMA, 100.0, THEATRE, 10.0));
        ScoredOffer cinema = scorer.score(offer(CINEMA, "20.00", "20.00", 0, OfferType.SINGLE, UUID.randomUUID()), profile, LoyaltyContext.none(), WEIGHTS, 20, 0, 3);
        ScoredOffer theatre = scorer.score(offer(THEATRE, "20.00", "20.00", 0, OfferType.SINGLE, UUID.randomUUID()), profile, LoyaltyContext.none(), WEIGHTS, 20, 0, 3);
        assertThat(cinema.score()).isGreaterThan(theatre.score());
        assertThat(cinema.reason().code()).isEqualTo("REASON_CATEGORY_INTEREST");
    }

    @Test
    void businessPriorityCannotLiftIrrelevantOfferAboveRelevantOne() {
        InterestProfile profile = profile(Map.of(CINEMA, 100.0));
        ScoredOffer relevant = scorer.score(offer(CINEMA, "20.00", "20.00", 0, OfferType.SINGLE, UUID.randomUUID()), profile, LoyaltyContext.none(), WEIGHTS, 20, 0, 3);
        ScoredOffer pushed = scorer.score(offer(THEATRE, "20.00", "20.00", 100, OfferType.SINGLE, UUID.randomUUID()), profile, LoyaltyContext.none(), WEIGHTS, 20, 0, 3);
        assertThat(relevant.score()).isGreaterThan(pushed.score());
    }

    @Test
    void offerThatReachesNextTierGetsLoyaltyReasonOnlyWhenRelevant() {
        LoyaltyContext loyalty = new LoyaltyContext(true, 15, "Silver", BigDecimal.ONE, 1, List.of());
        InterestProfile profile = profile(Map.of(CINEMA, 100.0));
        ScoredOffer relevant = scorer.score(offer(CINEMA, "22.00", "26.00", 0, OfferType.SINGLE, UUID.randomUUID()), profile, loyalty, WEIGHTS, 20, 0, 3);
        ScoredOffer irrelevant = scorer.score(offer(THEATRE, "22.00", "26.00", 0, OfferType.SINGLE, UUID.randomUUID()), profile, loyalty, WEIGHTS, 20, 0, 3);
        assertThat(relevant.reason().code()).isEqualTo("REASON_NEXT_BENEFIT");
        assertThat(relevant.component(RecommendationScorer.LOYALTY)).isEqualTo(100.0);
        assertThat(irrelevant.component(RecommendationScorer.LOYALTY)).isZero();
    }

    @Test
    void openBundleTaskBoostsBundles() {
        LoyaltyContext loyalty = new LoyaltyContext(true, 0, null, BigDecimal.ONE, 1,
                List.of(new LoyaltyContext.OpenTask("bundle-up", "Bundle up", TaskType.BUNDLE_PURCHASE, 0, 1, 25, Set.of())));
        ScoredOffer bundle = scorer.score(offer(CINEMA, "28.00", "35.00", 0, OfferType.BUNDLE, UUID.randomUUID()),
                profile(Map.of(CINEMA, 100.0)), loyalty, WEIGHTS, 20, 0, 3);
        assertThat(bundle.component(RecommendationScorer.LOYALTY)).isEqualTo(100.0);
        assertThat(bundle.reason().code()).isEqualTo("REASON_TASK_PROGRESS");
    }

    @Test
    void frequentlyServedOffersAreDemoted() {
        InterestProfile profile = profile(Map.of(CINEMA, 100.0));
        OfferSnapshot snapshot = offer(CINEMA, "20.00", "20.00", 0, OfferType.SINGLE, UUID.randomUUID());
        ScoredOffer fresh = scorer.score(snapshot, profile, LoyaltyContext.none(), WEIGHTS, 20, 0, 3);
        ScoredOffer stale = scorer.score(snapshot, profile, LoyaltyContext.none(), WEIGHTS, 20, 3, 3);
        assertThat(stale.score()).isLessThan(fresh.score());
    }

    @Test
    void budgetFitIsNeutralWithoutHistoryAndPeaksInsideBand() {
        assertThat(RecommendationScorer.budget(new BigDecimal("15.00"), InterestProfile.empty())).isEqualTo(50.0);
        InterestProfile band = new InterestProfile(Map.of(), Map.of(), Set.of(), new BigDecimal("10.00"), new BigDecimal("20.00"), 0, 5);
        assertThat(RecommendationScorer.budget(new BigDecimal("15.00"), band)).isEqualTo(100.0);
        assertThat(RecommendationScorer.budget(new BigDecimal("40.00"), band)).isZero();
    }

    @Test
    void reRankerCapsPartnersAndInsertsLabeledExploration() {
        UUID partner = UUID.randomUUID();
        InterestProfile profile = profile(Map.of(CINEMA, 100.0));
        List<ScoredOffer> personalized = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            personalized.add(scorer.score(offer(CINEMA, "20.00", "20.00", 0, OfferType.SINGLE, partner), profile, LoyaltyContext.none(), WEIGHTS, 20, 0, 3));
        }
        List<ScoredOffer> others = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            others.add(scorer.score(offer(UUID.randomUUID(), "18.00", "20.00", 0, OfferType.SINGLE, UUID.randomUUID()), profile, LoyaltyContext.none(), WEIGHTS, 20, 0, 3));
        }
        personalized.addAll(others);
        ScoredOffer explore = scorer.score(offer(THEATRE, "10.00", "20.00", 0, OfferType.SINGLE, UUID.randomUUID()), profile,
                LoyaltyContext.none(), WEIGHTS, 20, 0, 3);
        List<ScoredOffer> ranked = reRanker.rerank(personalized, List.of(explore), List.of(), 10, SETTINGS);

        Map<UUID, Integer> perPartner = new HashMap<>();
        ranked.stream().filter(item -> !item.exploration()).forEach(item -> perPartner.merge(item.offer().partners().getFirst().id(), 1, Integer::sum));
        assertThat(perPartner.get(partner)).isLessThanOrEqualTo(3);
        assertThat(ranked).anyMatch(item -> item.exploration() && item.reason().code().equals("REASON_TRY_NEW"));
        assertThat(ranked).hasSize(10);
    }

    private static InterestProfile profile(Map<UUID, Double> categories) {
        return new InterestProfile(categories, Map.of(), Set.of(CINEMA), null, null, 0, 10);
    }

    private static OfferSnapshot offer(UUID category, String price, String retail, int priority, OfferType type, UUID partner) {
        BigDecimal selling = new BigDecimal(price);
        BigDecimal reference = new BigDecimal(retail);
        BigDecimal savings = reference.subtract(selling);
        BigDecimal savingsPercent = savings.multiply(BigDecimal.valueOf(100)).divide(reference, 2, java.math.RoundingMode.HALF_UP);
        PriceBreakdown pricing = new PriceBreakdown(selling, reference, savings, savingsPercent, List.of(), BigDecimal.ONE, BigDecimal.ONE,
                0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, List.of(), true);
        return new OfferSnapshot(UUID.randomUUID(), "offer-" + UUID.randomUUID(), type, OfferVisibility.PUBLIC,
                new OfferSnapshot.CategoryRef(category, "slug", LocalizedText.of("a", "a"), "film"), List.of(category),
                List.of(new OfferSnapshot.PartnerRef(partner, "partner", "Partner", true)), LocalizedText.of("a", "a"),
                LocalizedText.of("a", "a"), null, pricing, true, 10, 5, Instant.now().plusSeconds(86_400),
                Instant.now().plusSeconds(86_400), null, Set.of("Baku"), Set.of(ProductType.ADMISSION), "a", priority, false, Instant.now(), 0,
                null, type == OfferType.BUNDLE ? 2 : 1);
    }
}
