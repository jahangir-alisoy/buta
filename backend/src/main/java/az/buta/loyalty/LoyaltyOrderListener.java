package az.buta.loyalty;

import az.buta.loyalty.task.TaskEngine;
import az.buta.order.domain.OrderRepository;
import az.buta.order.event.OrderCompletedEvent;
import az.buta.order.event.OrderFulfilledEvent;
import az.buta.order.event.OrderRefundedEvent;
import az.buta.order.event.RedemptionRedeemedEvent;
import az.buta.pricing.AwardTrigger;
import az.buta.pricing.BusinessSettingsService;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyOrderListener {

    private final OrderRepository orders;
    private final PointsService points;
    private final ReferralService referrals;
    private final TaskEngine taskEngine;
    private final BusinessSettingsService settings;

    public LoyaltyOrderListener(OrderRepository orders, PointsService points, ReferralService referrals, TaskEngine taskEngine,
                                BusinessSettingsService settings) {
        this.orders = orders;
        this.points = points;
        this.referrals = referrals;
        this.taskEngine = taskEngine;
        this.settings = settings;
    }

    @EventListener
    @Order(1)
    public void onFulfilled(OrderFulfilledEvent event) {
        az.buta.order.domain.Order order = orders.findById(event.orderId()).orElseThrow();
        if (settings.loyalty().awardTrigger() == AwardTrigger.FULFILLED) {
            points.awardForOrder(order);
            taskEngine.evaluate(event.userId(), event.fulfilledAt());
        }
        referrals.onOrderFulfilled(order);
    }

    @EventListener
    @Order(1)
    public void onCompleted(OrderCompletedEvent event) {
        if (settings.loyalty().awardTrigger() == AwardTrigger.COMPLETED) {
            points.awardForOrder(orders.findById(event.orderId()).orElseThrow());
            taskEngine.evaluate(event.userId(), event.completedAt());
        }
    }

    @EventListener
    @Order(1)
    public void onRefunded(OrderRefundedEvent event) {
        referrals.onOrderRefunded(event.orderId(), event.fullyRefunded());
        if (event.fulfilledAt() != null) {
            taskEngine.evaluate(event.userId(), event.fulfilledAt());
        }
    }

    @EventListener
    @Order(1)
    public void onRedeemed(RedemptionRedeemedEvent event) {
        taskEngine.evaluate(event.userId(), event.redeemedAt());
    }
}
