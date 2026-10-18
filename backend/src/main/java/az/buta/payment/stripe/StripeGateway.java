package az.buta.payment.stripe;

import az.buta.common.config.AppProperties;
import az.buta.common.money.Amounts;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripeGateway {

    private static final Set<String> FAILED_REFUND_STATUSES = Set.of("failed", "canceled");

    private final StripeClient client;
    private final String webhookSecret;

    public StripeGateway(StripeClient client, AppProperties properties) {
        this.client = client;
        this.webhookSecret = properties.payment().stripe().webhookSecret();
    }

    public String createCheckoutUrl(StripeCheckoutRequest request) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(request.successUrl())
                .setCancelUrl(request.cancelUrl())
                .setCustomerEmail(request.customerEmail())
                .setExpiresAt(request.expiresAt().getEpochSecond())
                .putAllMetadata(request.metadata())
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder().putAllMetadata(request.metadata()).build())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(Amounts.CURRENCY.toLowerCase(Locale.ROOT))
                                .setUnitAmount(minorUnits(request.amount()))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(request.productName())
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            return client.v1().checkout().sessions().create(params).getUrl();
        } catch (StripeException exception) {
            throw new StripeGatewayException(exception);
        }
    }

    public Session retrieveSession(String sessionId) {
        try {
            return client.v1().checkout().sessions().retrieve(sessionId);
        } catch (StripeException exception) {
            throw new StripeGatewayException(exception);
        }
    }

    public RefundOutcome refund(String paymentIntentId, BigDecimal amount, String idempotencyKey) {
        RefundCreateParams.Builder params = RefundCreateParams.builder().setPaymentIntent(paymentIntentId);
        if (amount != null) {
            params.setAmount(minorUnits(amount));
        }
        RequestOptions options = RequestOptions.builder().setIdempotencyKey(idempotencyKey).build();
        try {
            Refund refund = client.v1().refunds().create(params.build(), options);
            return new RefundOutcome(!FAILED_REFUND_STATUSES.contains(refund.getStatus()), refund.getId());
        } catch (StripeException exception) {
            throw new StripeGatewayException(exception);
        }
    }

    public Event verifiedEvent(String payload, String signature) throws SignatureVerificationException {
        return client.constructEvent(payload, signature, webhookSecret);
    }

    private static long minorUnits(BigDecimal amount) {
        return Amounts.scale(amount).movePointRight(2).longValueExact();
    }

    public record RefundOutcome(boolean succeeded, String reference) {
    }
}
