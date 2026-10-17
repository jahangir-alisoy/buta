package az.buta.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class InMemoryRateLimiter implements RateLimiter {

    private final Clock clock;
    private final Cache<String, Window> windows = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(2))
            .maximumSize(200_000)
            .build();

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public RateLimitDecision tryAcquire(String key, int limit, Duration window) {
        Instant now = clock.instant();
        AtomicReference<RateLimitDecision> decision = new AtomicReference<>();
        windows.asMap().compute(key, (ignored, current) -> {
            Window active = current == null || !now.isBefore(current.start().plus(window)) ? new Window(now, 0) : current;
            if (active.count() >= limit) {
                decision.set(new RateLimitDecision(false, Duration.between(now, active.start().plus(window))));
                return active;
            }
            decision.set(RateLimitDecision.allow());
            return new Window(active.start(), active.count() + 1);
        });
        return decision.get();
    }

    public void reset() {
        windows.invalidateAll();
    }

    private record Window(Instant start, int count) {
    }
}
