package az.buta.payment.stripe;

import az.buta.payment.Payment;
import az.buta.payment.PaymentProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripePaymentProvider implements PaymentProvider {

    private final StripeGateway gateway;

    public StripePaymentProvider(StripeGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public String code() {
        return "STRIPE";
    }

    @Override
    public String mode() {
        return "STRIPE";
    }

    @Override
    public String createPaymentReference(String orderNumber, BigDecimal amount) {
        return "stripe-pending-" + UUID.randomUUID();
    }

    @Override
    public RefundResult refund(Payment payment, BigDecimal amount) {
        StripeGateway.RefundOutcome outcome = gateway.refund(payment.providerReference(), amount, UUID.randomUUID().toString());
        return new RefundResult(outcome.succeeded(), outcome.reference());
    }
}
