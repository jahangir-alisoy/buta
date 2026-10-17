package az.buta.loyalty.task;

import java.time.Instant;

public record TaskWindow(String periodKey, Instant start, Instant end) {

    public boolean contains(Instant instant) {
        return instant != null && !instant.isBefore(start) && instant.isBefore(end);
    }
}
