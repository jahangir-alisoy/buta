package az.buta.common.error;

import java.time.Duration;

public class RateLimitedException extends RuntimeException {

    private final ErrorCode code;
    private final transient Duration retryAfter;

    public RateLimitedException(ErrorCode code, Duration retryAfter) {
        super(code.name());
        this.code = code;
        this.retryAfter = retryAfter;
    }

    public ErrorCode code() {
        return code;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
