package az.buta.recommendation;

import az.buta.catalog.OfferSnapshot;

import java.util.Map;

public record ScoredOffer(OfferSnapshot offer, Map<String, Double> components, double score, Reason reason, boolean exploration) {

    public double component(String name) {
        return components.getOrDefault(name, 0.0);
    }

    public ScoredOffer asExploration() {
        return new ScoredOffer(offer, components, score, new Reason("REASON_TRY_NEW", Map.of("category", offer.primaryCategory().slug())), true);
    }

    public ScoredOffer withReason(Reason newReason) {
        return new ScoredOffer(offer, components, score, newReason, exploration);
    }

    public record Reason(String code, Map<String, String> params) {
    }
}
