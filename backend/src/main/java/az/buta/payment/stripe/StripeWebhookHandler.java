package az.buta.payment.stripe;

import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.membership.PlusSubscriptionService;
import az.buta.order.OrderLifecycleService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripeWebhookHandler {

    private static final Set<String> COMPLETION_EVENTS = Set.of("checkout.session.completed", "checkout.session.async_payment_succeeded");

    private final StripeGateway gateway;
    private final OrderLifecycleService lifecycle;
    private final PlusSubscriptionService plusSubscriptions;

    public StripeWebhookHandler(StripeGateway gateway, OrderLifecycleService lifecycle, PlusSubscriptionService plusSubscriptions) {
        this.gateway = gateway;
        this.lifecycle = lifecycle;
        this.plusSubscriptions = plusSubscriptions;
    }

    public void handle(String payload, String signature) {
        Event event = verified(payload, signature);
        if (!COMPLETION_EVENTS.contains(event.getType())) {
            return;
        }
        Session session = session(event.getDataObjectDeserializer());
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

    private Event verified(String payload, String signature) {
        try {
            return gateway.verifiedEvent(payload, signature);
        } catch (SignatureVerificationException exception) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
    }

    private static Session session(EventDataObjectDeserializer deserializer) {
        try {
            Optional<StripeObject> object = deserializer.getObject();
            return (Session) (object.isPresent() ? object.get() : deserializer.deserializeUnsafe());
        } catch (EventDataObjectDeserializationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
