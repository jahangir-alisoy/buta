package az.buta.order;

import az.buta.audit.AuditService;
import az.buta.common.error.ApiException;
import az.buta.order.domain.Order;
import az.buta.order.domain.Redemption;
import az.buta.order.domain.RedemptionRepository;
import az.buta.order.event.OrderCompletedEvent;
import az.buta.order.event.RedemptionRedeemedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class RedemptionService {

    private final RedemptionRepository redemptions;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RedemptionService(RedemptionRepository redemptions, AuditService audit, ApplicationEventPublisher events, Clock clock) {
        this.redemptions = redemptions;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public RedemptionResult redeem(UUID adminId, String rawCode) {
        String code = rawCode == null ? "" : rawCode.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        Redemption redemption = redemptions.findByCode(code).orElseThrow(ApiException::notFound);
        Instant now = clock.instant();
        Order order = redemption.orderItem().order();
        if (!order.isPaid()) {
            throw ApiException.of(az.buta.common.error.ErrorCode.ORDER_STATE_INVALID);
        }
        boolean changed = redemption.redeem(adminId, now);
        if (changed) {
            audit.admin(adminId, "REDEMPTION_REDEEMED", "REDEMPTION", redemption.getId(), Map.of("orderNumber", order.orderNumber()));
            events.publishEvent(new RedemptionRedeemedEvent(redemption.getId(), order.getId(), order.userId(), now));
            if (order.completeIfSettled(now)) {
                events.publishEvent(new OrderCompletedEvent(order.getId(), order.userId(), now));
            }
        }
        return new RedemptionResult(code, redemption.status().name(), order.orderNumber(), changed);
    }

    @Transactional
    public int expireDue() {
        Instant now = clock.instant();
        int expired = 0;
        for (Redemption redemption : redemptions.findDueForExpiry(now)) {
            if (redemption.expireIfDue(now)) {
                expired++;
                Order order = redemption.orderItem().order();
                if (order.completeIfSettled(now)) {
                    events.publishEvent(new OrderCompletedEvent(order.getId(), order.userId(), now));
                }
            }
        }
        return expired;
    }

    public record RedemptionResult(String code, String status, String orderNumber, boolean changed) {
    }
}
