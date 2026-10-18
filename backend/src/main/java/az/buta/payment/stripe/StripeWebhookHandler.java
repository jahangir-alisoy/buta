package az.buta.payment.stripe;

import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripeWebhookHandler {

    private static final Set<String> COMPLETION_EVENTS = Set.of("checkout.session.completed", "checkout.session.async_payment_succeeded");

    private final StripeGateway gateway;
    private final StripeSessionCompletion completion;

    public StripeWebhookHandler(StripeGateway gateway, StripeSessionCompletion completion) {
        this.gateway = gateway;
        this.completion = completion;
    }

    public void handle(String payload, String signature) {
        Event event = verified(payload, signature);
        if (COMPLETION_EVENTS.contains(event.getType())) {
            completion.complete(session(event.getDataObjectDeserializer()));
        }
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
