package az.buta.loyalty.task;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PurchaseFact(UUID orderId, UUID orderItemId, boolean bundle, UUID categoryId, Set<UUID> partnerIds,
                           Instant purchasedAt, BigDecimal orderTotal) {
}
