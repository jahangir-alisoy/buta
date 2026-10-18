package az.buta.payment.stripe;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record StripeCheckoutRequest(String productName, BigDecimal amount, String customerEmail, Map<String, String> metadata,
                                    String successUrl, String cancelUrl, Instant expiresAt) {
}
