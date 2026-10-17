package az.buta.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "sandbox", matchIfMissing = true)
public class SandboxPaymentProvider implements PaymentProvider {

    @Override
    public String code() {
        return "SANDBOX";
    }

    @Override
    public String mode() {
        return "SANDBOX";
    }

    @Override
    public String createPaymentReference(String orderNumber, BigDecimal amount) {
        return "sandbox-" + UUID.randomUUID();
    }

    @Override
    public RefundResult refund(Payment payment, BigDecimal amount) {
        return new RefundResult(true, "sandbox-refund-" + UUID.randomUUID());
    }
}
