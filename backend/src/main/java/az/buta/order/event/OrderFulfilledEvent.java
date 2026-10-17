package az.buta.order.event;

import java.time.Instant;
import java.util.UUID;

public record OrderFulfilledEvent(UUID orderId, UUID userId, Instant fulfilledAt) {
}
