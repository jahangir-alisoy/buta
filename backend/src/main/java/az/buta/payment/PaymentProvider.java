package az.buta.payment;

import java.math.BigDecimal;

public interface PaymentProvider {

    String code();

    String mode();

    String createPaymentReference(String orderNumber, BigDecimal amount);

    RefundResult refund(Payment payment, BigDecimal amount);

    record RefundResult(boolean succeeded, String reference) {
    }
}
