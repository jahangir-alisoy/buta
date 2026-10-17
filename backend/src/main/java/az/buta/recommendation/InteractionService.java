package az.buta.recommendation;

import az.buta.recommendation.domain.InteractionEvent;
import az.buta.recommendation.domain.InteractionEventRepository;
import az.buta.recommendation.domain.InteractionType;
import az.buta.user.domain.UserPreferences;
import az.buta.user.domain.UserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class InteractionService {

    private static final Set<InteractionType> ANONYMOUS_ALLOWED = Set.of(InteractionType.DETAIL_VIEW);

    private final InteractionEventRepository events;
    private final UserPreferencesRepository preferences;
    private final Clock clock;

    public InteractionService(InteractionEventRepository events, UserPreferencesRepository preferences, Clock clock) {
        this.events = events;
        this.preferences = preferences;
        this.clock = clock;
    }

    @Transactional
    public boolean record(UUID userId, InteractionType type, UUID offerId, UUID categoryId, String query, String requestId, String placement) {
        Instant now = clock.instant();
        if (userId == null) {
            if (!ANONYMOUS_ALLOWED.contains(type)) {
                return false;
            }
            events.save(new InteractionEvent(null, type, offerId, categoryId, null, null, placement, now));
            return true;
        }
        if (!personalizationEnabled(userId)) {
            return false;
        }
        if (type == InteractionType.DETAIL_VIEW && offerId != null && events.existsSince(userId, offerId, type, now.minus(Duration.ofDays(1)))) {
            return false;
        }
        String safeQuery = query == null ? null : query.strip().substring(0, Math.min(100, query.strip().length()));
        events.save(new InteractionEvent(userId, type, offerId, categoryId, safeQuery, requestId, placement, now));
        return true;
    }

    @Transactional(readOnly = true)
    public boolean personalizationEnabled(UUID userId) {
        return preferences.findById(userId).map(UserPreferences::personalizationEnabled).orElse(false);
    }

    @Transactional
    public int purgeExpired(int retentionDays) {
        return events.purgeBefore(clock.instant().minus(Duration.ofDays(retentionDays)));
    }
}
