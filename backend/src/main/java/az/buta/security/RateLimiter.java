package az.buta.security;

import java.time.Duration;

public interface RateLimiter {

    RateLimitDecision tryAcquire(String key, int limit, Duration window);
}
