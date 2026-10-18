package az.buta.payment.stripe;

import az.buta.common.config.AppProperties;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.membership.PlusSubscriptionService;
import az.buta.order.domain.Order;
import az.buta.order.domain.OrderRepository;
import az.buta.order.domain.OrderStatus;
import az.buta.payment.Payment;
import az.buta.payment.PaymentRepository;
import az.buta.payment.PaymentStatus;
import az.buta.security.AuthenticatedUser;
import com.stripe.model.checkout.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripeCheckoutService {

    static final String PURPOSE = "purpose";
    static final String ORDER = "ORDER";
    static final String PLUS = "PLUS";
    static final String PAYMENT_ID = "paymentId";
    static final String USER_ID = "userId";
    private static final String SESSION_ID_PARAMETER = "&session_id={CHECKOUT_SESSION_ID}";
    private static final Duration MIN_SESSION_LIFETIME = Duration.ofMinutes(31);

    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final PlusSubscriptionService plusSubscriptions;
    private final StripeGateway gateway;
    private final StripeSessionCompletion completion;
    private final String publicBaseUrl;
    private final Clock clock;

    public StripeCheckoutService(PaymentRepository payments, OrderRepository orders, PlusSubscriptionService plusSubscriptions,
                                 StripeGateway gateway, StripeSessionCompletion completion, AppProperties properties, Clock clock) {
        this.payments = payments;
        this.orders = orders;
        this.plusSubscriptions = plusSubscriptions;
        this.gateway = gateway;
        this.completion = completion;
        this.publicBaseUrl = properties.publicBaseUrl().replaceAll("/+$", "");
        this.clock = clock;
    }

    public StripeCheckoutDto orderCheckout(AuthenticatedUser user, UUID paymentId) {
        Payment payment = payments.findById(paymentId).orElseThrow(ApiException::notFound);
        Order order = orders.findById(payment.orderId()).filter(candidate -> candidate.userId().equals(user.userId()))
                .orElseThrow(ApiException::notFound);
        Instant now = clock.instant();
        if (payment.status() != PaymentStatus.PENDING || order.status() != OrderStatus.PENDING_PAYMENT || !now.isBefore(order.expiresAt())) {
            throw ApiException.of(ErrorCode.ORDER_STATE_INVALID);
        }
        String returnUrl = publicBaseUrl + "/checkout/pay/" + order.getId();
        return new StripeCheckoutDto(gateway.createCheckoutUrl(new StripeCheckoutRequest(order.orderNumber(), payment.amount(), user.email(),
                Map.of(PURPOSE, ORDER, PAYMENT_ID, payment.getId().toString()), returnUrl + "?stripe=success" + SESSION_ID_PARAMETER, returnUrl,
                sessionExpiry(order.expiresAt(), now))));
    }

    public StripeCheckoutDto plusCheckout(AuthenticatedUser user) {
        Instant now = clock.instant();
        String returnUrl = publicBaseUrl + "/rewards";
        return new StripeCheckoutDto(gateway.createCheckoutUrl(new StripeCheckoutRequest("Naxış Plus", plusSubscriptions.purchasePrice(user),
                user.email(), Map.of(PURPOSE, PLUS, USER_ID, user.userId().toString()), returnUrl + "?plus=success" + SESSION_ID_PARAMETER, returnUrl,
                now.plus(MIN_SESSION_LIFETIME))));
    }

    public void confirm(AuthenticatedUser user, String sessionId) {
        Session session = gateway.retrieveSession(sessionId);
        if (!belongsTo(session.getMetadata(), user.userId())) {
            throw ApiException.notFound();
        }
        completion.complete(session);
    }

    private boolean belongsTo(Map<String, String> metadata, UUID userId) {
        return switch (metadata.getOrDefault(PURPOSE, "")) {
            case ORDER -> payments.findById(UUID.fromString(metadata.get(PAYMENT_ID)))
                    .flatMap(payment -> orders.findById(payment.orderId()))
                    .filter(order -> order.userId().equals(userId))
                    .isPresent();
            case PLUS -> userId.toString().equals(metadata.get(USER_ID));
            default -> false;
        };
    }

    private static Instant sessionExpiry(Instant orderExpiresAt, Instant now) {
        Instant earliest = now.plus(MIN_SESSION_LIFETIME);
        return orderExpiresAt.isAfter(earliest) ? orderExpiresAt : earliest;
    }
}
