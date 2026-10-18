package az.buta.payment.stripe;

import az.buta.security.AuthenticatedUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripeCheckoutController {

    private final StripeCheckoutService checkouts;

    public StripeCheckoutController(StripeCheckoutService checkouts) {
        this.checkouts = checkouts;
    }

    @PostMapping("/payments/{paymentId}/stripe-checkout")
    public StripeCheckoutDto orderCheckout(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID paymentId) {
        return checkouts.orderCheckout(user, paymentId);
    }

    @PostMapping("/payments/stripe/sessions/{sessionId}/confirmation")
    public void confirm(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String sessionId) {
        checkouts.confirm(user, sessionId);
    }

    @PostMapping("/me/plus/stripe-checkout")
    public StripeCheckoutDto plusCheckout(@AuthenticationPrincipal AuthenticatedUser user) {
        return checkouts.plusCheckout(user);
    }
}
