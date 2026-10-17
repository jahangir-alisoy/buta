package az.buta.order.event;

import java.time.Instant;
import java.util.UUID;

public record RedemptionRedeemedEvent(UUID redemptionId, UUID orderId, UUID userId, Instant redeemedAt) {
}
