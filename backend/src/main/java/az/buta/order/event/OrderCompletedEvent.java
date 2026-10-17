package az.buta.order.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCompletedEvent(UUID orderId, UUID userId, Instant completedAt) {
}
