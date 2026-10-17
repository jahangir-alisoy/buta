package az.buta.membership;

import az.buta.order.event.OrderFulfilledEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class MembershipOrderListener {

    private final MembershipService membership;

    public MembershipOrderListener(MembershipService membership) {
        this.membership = membership;
    }

    @EventListener
    @Order(2)
    public void onFulfilled(OrderFulfilledEvent event) {
        membership.evaluateEarnedPlus(event.userId());
    }
}
