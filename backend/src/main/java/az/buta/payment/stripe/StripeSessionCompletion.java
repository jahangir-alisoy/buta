package az.buta.payment.stripe;

import az.buta.membership.PlusSubscriptionService;
import az.buta.order.OrderLifecycleService;
import com.stripe.model.checkout.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripeSessionCompletion {

    private final StripeGateway gateway;
    private final OrderLifecycleService lifecycle;
    private final PlusSubscriptionService plusSubscriptions;

    public StripeSessionCompletion(StripeGateway gateway, OrderLifecycleService lifecycle, PlusSubscriptionService plusSubscriptions) {
        this.gateway = gateway;
        this.lifecycle = lifecycle;
        this.plusSubscriptions = plusSubscriptions;
    }

    public void complete(Session session) {
        if (!"paid".equals(session.getPaymentStatus())) {
            return;
        }
        if (!accept(session.getMetadata(), session.getPaymentIntent())) {
            gateway.refund(session.getPaymentIntent(), null, "full-refund-" + session.getPaymentIntent());
        }
    }

    private boolean accept(Map<String, String> metadata, String paymentIntentId) {
        return switch (metadata.getOrDefault(StripeCheckoutService.PURPOSE, "")) {
            case StripeCheckoutService.ORDER ->
                    lifecycle.confirmProviderPayment(UUID.fromString(metadata.get(StripeCheckoutService.PAYMENT_ID)), paymentIntentId);
            case StripeCheckoutService.PLUS ->
                    plusSubscriptions.completeProviderPurchase(UUID.fromString(metadata.get(StripeCheckoutService.USER_ID)), paymentIntentId);
            default -> true;
        };
    }
}
