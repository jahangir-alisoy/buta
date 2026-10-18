package az.buta.payment.stripe;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripeWebhookController {

    private final StripeWebhookHandler webhooks;

    public StripeWebhookController(StripeWebhookHandler webhooks) {
        this.webhooks = webhooks;
    }

    @PostMapping("/api/v1/payments/stripe/webhook")
    public void webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        webhooks.handle(payload, signature);
    }
}
