package az.buta.order;

import az.buta.cart.CartService;
import az.buta.catalog.InventoryService;
import az.buta.catalog.OfferCatalog;
import az.buta.catalog.OfferPopularityService;
import az.buta.catalog.domain.PartnerProduct;
import az.buta.catalog.domain.PartnerProductRepository;
import az.buta.common.crypto.SecureCodes;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.loyalty.PointsService;
import az.buta.order.domain.Order;
import az.buta.order.domain.OrderItem;
import az.buta.order.domain.OrderItemComponent;
import az.buta.order.domain.OrderRepository;
import az.buta.order.domain.OrderStatus;
import az.buta.order.domain.Redemption;
import az.buta.order.domain.RedemptionRepository;
import az.buta.order.dto.OrderDto;
import az.buta.order.dto.SandboxConfirmationRequest;
import az.buta.order.event.OrderFulfilledEvent;
import az.buta.payment.Payment;
import az.buta.payment.PaymentProvider;
import az.buta.payment.PaymentRepository;
import az.buta.payment.PaymentStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderLifecycleService {

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final RedemptionRepository redemptions;
    private final PartnerProductRepository products;
    private final InventoryService inventory;
    private final PointsService points;
    private final CartService carts;
    private final OfferCatalog catalog;
    private final OfferPopularityService popularity;
    private final PaymentProvider paymentProvider;
    private final OrderViewAssembler views;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderLifecycleService(OrderRepository orders, PaymentRepository payments, RedemptionRepository redemptions,
                                 PartnerProductRepository products, InventoryService inventory, PointsService points, CartService carts,
                                 OfferCatalog catalog, OfferPopularityService popularity, PaymentProvider paymentProvider,
                                 OrderViewAssembler views, ApplicationEventPublisher events, Clock clock) {
        this.orders = orders;
        this.payments = payments;
        this.redemptions = redemptions;
        this.products = products;
        this.inventory = inventory;
        this.points = points;
        this.carts = carts;
        this.catalog = catalog;
        this.popularity = popularity;
        this.paymentProvider = paymentProvider;
        this.views = views;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public OrderDto confirmSandboxPayment(UUID userId, UUID paymentId, SandboxConfirmationRequest.Outcome outcome, Locale locale) {
        if (!"SANDBOX".equals(paymentProvider.mode())) {
            throw ApiException.notFound();
        }
        Payment payment = payments.findById(paymentId).orElseThrow(ApiException::notFound);
        Order order = orders.findForUpdate(payment.orderId()).filter(candidate -> candidate.userId().equals(userId))
                .orElseThrow(ApiException::notFound);
        boolean success = outcome == SandboxConfirmationRequest.Outcome.SUCCEEDED;
        if (payment.status() != PaymentStatus.PENDING) {
            boolean alreadyDone = (success && payment.status() == PaymentStatus.CAPTURED) || (!success && payment.status() == PaymentStatus.FAILED);
            if (alreadyDone) {
                return views.toDto(order, locale);
            }
            throw ApiException.of(ErrorCode.ORDER_STATE_INVALID);
        }
        Instant now = clock.instant();
        if (order.status() != OrderStatus.PENDING_PAYMENT || !now.isBefore(order.expiresAt())) {
            throw ApiException.of(ErrorCode.ORDER_STATE_INVALID);
        }
        if (success) {
            fulfill(order, payment, now);
        } else {
            payment.fail("SANDBOX_DECLINED", now);
            release(order, now, "PAYMENT_FAILED");
        }
        return views.toDto(order, locale);
    }

    @Transactional
    public OrderDto cancelByCustomer(UUID userId, UUID orderId, Locale locale) {
        Order order = orders.findForUpdate(orderId).filter(candidate -> candidate.userId().equals(userId))
                .orElseThrow(ApiException::notFound);
        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw ApiException.of(ErrorCode.ORDER_STATE_INVALID);
        }
        Instant now = clock.instant();
        payments.findByOrderId(orderId).ifPresent(payment -> payment.abandon(now));
        release(order, now, "CUSTOMER_CANCELLED");
        return views.toDto(order, locale);
    }

    @Transactional
    public int expireDuePending(int batchSize) {
        Instant now = clock.instant();
        List<UUID> ids = orders.lockExpiredPending(now, batchSize);
        for (UUID id : ids) {
            Order order = orders.findById(id).orElseThrow();
            payments.findByOrderId(id).ifPresent(payment -> payment.abandon(now));
            release(order, now, "RESERVATION_EXPIRED");
        }
        return ids.size();
    }

    private void fulfill(Order order, Payment payment, Instant now) {
        payment.capture(now);
        inventory.sellAll(componentQuantities(order), order.getId());
        Set<UUID> productIds = order.items().stream().flatMap(item -> item.components().stream())
                .map(OrderItemComponent::productId).collect(Collectors.toSet());
        Map<UUID, PartnerProduct> productMap = products.findAllById(productIds).stream()
                .collect(Collectors.toMap(PartnerProduct::getId, Function.identity()));
        for (OrderItem item : order.items()) {
            for (OrderItemComponent component : item.components()) {
                PartnerProduct product = productMap.get(component.productId());
                for (int unit = 0; unit < component.quantity(); unit++) {
                    item.addRedemption(new Redemption(item, product.getId(), uniqueCode(), product.validFrom(), product.validUntil(), now));
                }
            }
        }
        order.markFulfilled(now);
        orders.saveAndFlush(order);
        catalog.evict();
        popularity.evict();
        events.publishEvent(new OrderFulfilledEvent(order.getId(), order.userId(), now));
    }

    private void release(Order order, Instant now, String reason) {
        inventory.releaseAll(componentQuantities(order), order.getId());
        points.reverseOrderRedemption(order.getId());
        if ("RESERVATION_EXPIRED".equals(reason)) {
            order.expire(now);
        } else {
            order.cancel(reason, now);
        }
        if (!"RESERVATION_EXPIRED".equals(reason)) {
            carts.restore(order.userId(), order.items().stream()
                    .map(item -> new CartService.RestoredItem(item.offerId(), item.quantity(), item.unitPrice(), item.recommendationRequestId()))
                    .toList());
        }
        catalog.evict();
    }

    private static Map<UUID, Integer> componentQuantities(Order order) {
        Map<UUID, Integer> quantities = new HashMap<>();
        order.items().forEach(item -> item.components()
                .forEach(component -> quantities.merge(component.productId(), component.quantity(), Integer::sum)));
        return quantities;
    }

    private String uniqueCode() {
        String code = SecureCodes.crockford(12);
        while (redemptions.existsByCode(code)) {
            code = SecureCodes.crockford(12);
        }
        return code;
    }
}
