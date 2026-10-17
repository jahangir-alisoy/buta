package az.buta.recommendation;

import az.buta.pricing.RecommendationSettings;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class RecommendationReRanker {

    public List<ScoredOffer> rerank(List<ScoredOffer> personalized, List<ScoredOffer> exploration, List<ScoredOffer> fallback, int limit,
                                    RecommendationSettings settings) {
        int explorationSlots = limit >= 6 && settings.explorationPercent() > 0
                ? Math.max(1, (int) Math.floor(limit * settings.explorationPercent() / 100.0))
                : 0;
        List<ScoredOffer> result = new ArrayList<>();
        Set<UUID> used = new HashSet<>();
        Map<UUID, Integer> perPartner = new HashMap<>();
        Map<UUID, Integer> perCategory = new HashMap<>();
        List<ScoredOffer> explorationQueue = new ArrayList<>(exploration);
        int nextExplorationIndex = 3;
        int explorationUsed = 0;
        List<ScoredOffer> primary = new ArrayList<>(personalized);
        primary.addAll(fallback);
        for (int pass = 0; pass < 2 && result.size() < limit; pass++) {
            boolean enforceDiversity = pass == 0;
            for (ScoredOffer candidate : primary) {
                if (result.size() >= limit) {
                    break;
                }
                while (explorationUsed < explorationSlots && result.size() == nextExplorationIndex && !explorationQueue.isEmpty()) {
                    ScoredOffer explore = explorationQueue.removeFirst();
                    if (used.add(explore.offer().id())) {
                        result.add(explore.asExploration());
                        count(explore, perPartner, perCategory);
                        explorationUsed++;
                        nextExplorationIndex += 7;
                    }
                }
                if (result.size() >= limit || used.contains(candidate.offer().id())) {
                    continue;
                }
                if (enforceDiversity && result.size() < 10 && violates(candidate, perPartner, perCategory, settings.diversity())) {
                    continue;
                }
                used.add(candidate.offer().id());
                result.add(candidate);
                count(candidate, perPartner, perCategory);
            }
        }
        while (result.size() < limit && explorationUsed < explorationSlots && !explorationQueue.isEmpty()) {
            ScoredOffer explore = explorationQueue.removeFirst();
            if (used.add(explore.offer().id())) {
                result.add(explore.asExploration());
                explorationUsed++;
            }
        }
        return result;
    }

    private static boolean violates(ScoredOffer candidate, Map<UUID, Integer> perPartner, Map<UUID, Integer> perCategory,
                                    RecommendationSettings.Diversity diversity) {
        boolean partnerFull = candidate.offer().partners().stream()
                .anyMatch(partner -> perPartner.getOrDefault(partner.id(), 0) >= diversity.maxPerPartnerTop10());
        boolean categoryFull = perCategory.getOrDefault(candidate.offer().primaryCategory().id(), 0) >= diversity.maxPerCategoryTop10();
        return partnerFull || categoryFull;
    }

    private static void count(ScoredOffer offer, Map<UUID, Integer> perPartner, Map<UUID, Integer> perCategory) {
        offer.offer().partners().forEach(partner -> perPartner.merge(partner.id(), 1, Integer::sum));
        perCategory.merge(offer.offer().primaryCategory().id(), 1, Integer::sum);
    }
}
