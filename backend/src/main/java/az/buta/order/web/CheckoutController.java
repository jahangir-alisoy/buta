package az.buta.order.web;

import az.buta.catalog.ViewerResolver;
import az.buta.order.CheckoutService;
import az.buta.order.OrderLifecycleService;
import az.buta.order.dto.CheckoutRequest;
import az.buta.order.dto.OrderDto;
import az.buta.order.dto.QuoteDto;
import az.buta.order.dto.SandboxConfirmationRequest;
import az.buta.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CheckoutController {

    private final CheckoutService checkout;
    private final OrderLifecycleService lifecycle;
    private final ViewerResolver viewers;

    public CheckoutController(CheckoutService checkout, OrderLifecycleService lifecycle, ViewerResolver viewers) {
        this.checkout = checkout;
        this.lifecycle = lifecycle;
        this.viewers = viewers;
    }

    @PostMapping("/checkout/quote")
    public QuoteDto quote(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CheckoutRequest request) {
        return checkout.quote(viewers.resolve(user), request.pointsToRedeem());
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto place(@AuthenticationPrincipal AuthenticatedUser user, @RequestHeader("Idempotency-Key") String idempotencyKey,
                          @Valid @RequestBody CheckoutRequest request) {
        return checkout.place(viewers.resolve(user), request.pointsToRedeem(), idempotencyKey);
    }

    @PostMapping("/payments/{paymentId}/sandbox-confirmation")
    public OrderDto confirm(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID paymentId,
                            @Valid @RequestBody SandboxConfirmationRequest request) {
        return lifecycle.confirmSandboxPayment(user.userId(), paymentId, request.outcome(), LocaleContextHolder.getLocale());
    }
}
