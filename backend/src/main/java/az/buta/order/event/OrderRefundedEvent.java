package az.buta.order.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderRefundedEvent(UUID orderId, UUID userId, List<UUID> refundedItemIds, Instant fulfilledAt, boolean fullyRefunded) {
}
