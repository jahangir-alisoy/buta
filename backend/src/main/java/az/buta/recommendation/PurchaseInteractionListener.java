package az.buta.recommendation;

import az.buta.order.domain.OrderRepository;
import az.buta.order.event.OrderFulfilledEvent;
import az.buta.recommendation.domain.InteractionType;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class PurchaseInteractionListener {

    private final OrderRepository orders;
    private final InteractionService interactions;

    public PurchaseInteractionListener(OrderRepository orders, InteractionService interactions) {
        this.orders = orders;
        this.interactions = interactions;
    }

    @EventListener
    @Order(3)
    public void onFulfilled(OrderFulfilledEvent event) {
        orders.findById(event.orderId()).ifPresent(order -> order.items().forEach(item ->
                interactions.record(order.userId(), InteractionType.PURCHASE, item.offerId(), item.categoryId(), null,
                        item.recommendationRequestId(), null)));
    }
}
