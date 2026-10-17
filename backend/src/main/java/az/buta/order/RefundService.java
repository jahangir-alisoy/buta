package az.buta.order;

import az.buta.audit.AuditService;
import az.buta.catalog.InventoryService;
import az.buta.catalog.OfferCatalog;
import az.buta.catalog.domain.PartnerProduct;
import az.buta.catalog.domain.PartnerProductRepository;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.money.Amounts;
import az.buta.loyalty.PointsService;
import az.buta.order.domain.Order;
import az.buta.order.domain.OrderItem;
import az.buta.order.domain.OrderItemComponent;
import az.buta.order.domain.OrderRepository;
import az.buta.order.domain.OrderStatus;
import az.buta.order.domain.Redemption;
import az.buta.order.domain.Refund;
import az.buta.order.domain.RefundInitiator;
import az.buta.order.domain.RefundRepository;
import az.buta.order.dto.OrderDto;
import az.buta.order.event.OrderRefundedEvent;
import az.buta.payment.Payment;
import az.buta.payment.PaymentProvider;
import az.buta.payment.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RefundService {

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final PartnerProductRepository products;
    private final PaymentProvider paymentProvider;
    private final InventoryService inventory;
    private final PointsService points;
    private final RefundPolicy policy;
    private final OrderViewAssembler views;
    private final OfferCatalog catalog;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RefundService(OrderRepository orders, PaymentRepository payments, RefundRepository refunds, PartnerProductRepository products,
                         PaymentProvider paymentProvider, InventoryService inventory, PointsService points, RefundPolicy policy,
                         OrderViewAssembler views, OfferCatalog catalog, AuditService audit, ApplicationEventPublisher events, Clock clock) {
        this.orders = orders;
        this.payments = payments;
        this.refunds = refunds;
        this.products = products;
        this.paymentProvider = paymentProvider;
        this.inventory = inventory;
        this.points = points;
        this.policy = policy;
        this.views = views;
        this.catalog = catalog;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public OrderDto refundByCustomer(UUID userId, UUID orderId, List<UUID> itemIds, String reason, Locale locale) {
        Order order = orders.findForUpdate(orderId).filter(candidate -> candidate.userId().equals(userId)).orElseThrow(ApiException::notFound);
        refund(order, itemIds, reason == null || reason.isBlank() ? "CUSTOMER_REQUEST" : reason.strip(), RefundInitiator.CUSTOMER, userId, false);
        return views.toDto(order, locale);
    }

    @Transactional
    public OrderDto refundByAdmin(UUID adminId, UUID orderId, List<UUID> itemIds, String reason, Locale locale) {
        if (reason == null || reason.isBlank()) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "reason");
        }
        Order order = orders.findForUpdate(orderId).orElseThrow(ApiException::notFound);
        refund(order, itemIds, reason.strip(), RefundInitiator.ADMIN, adminId, true);
        audit.admin(adminId, "ORDER_REFUNDED", "ORDER", orderId, Map.of("reason", reason.strip()));
        return views.toDto(order, locale, true);
    }

    private void refund(Order order, List<UUID> requestedItemIds, String reason, RefundInitiator initiator, UUID actorId, boolean admin) {
        Instant now = clock.instant();
        Set<UUID> productIds = order.items().stream().flatMap(item -> item.redemptions().stream()).map(Redemption::productId)
                .collect(Collectors.toSet());
        Map<UUID, PartnerProduct> productMap = products.findAllById(productIds).stream()
                .collect(Collectors.toMap(PartnerProduct::getId, Function.identity()));
        boolean explicit = requestedItemIds != null && !requestedItemIds.isEmpty();
        List<OrderItem> candidates = order.items().stream()
                .filter(item -> !explicit || requestedItemIds.contains(item.getId()))
                .toList();
        if (explicit && candidates.size() != requestedItemIds.size()) {
            throw ApiException.notFound();
        }
        List<OrderItem> eligible = candidates.stream()
                .filter(item -> policy.blockReason(order, item, productMap, now, admin) == null)
                .toList();
        if (eligible.isEmpty() || (explicit && eligible.size() != candidates.size())) {
            throw ApiException.of(ErrorCode.REFUND_NOT_ALLOWED);
        }
        Payment payment = payments.findByOrderId(order.getId()).orElseThrow(() -> ApiException.of(ErrorCode.ORDER_STATE_INVALID));
        BigDecimal amount = Amounts.scale(eligible.stream().map(OrderItem::cashPaid).reduce(BigDecimal.ZERO, BigDecimal::add));
        PaymentProvider.RefundResult result = paymentProvider.refund(payment, amount);
        if (!result.succeeded()) {
            refunds.save(new Refund(order.getId(), payment.getId(), amount, reason, initiator, actorId, "FAILED", result.reference(), now));
            throw ApiException.of(ErrorCode.REFUND_NOT_ALLOWED);
        }
        payment.recordRefund(amount, now);
        for (OrderItem item : eligible) {
            item.markRefunded();
            item.redemptions().forEach(redemption -> redemption.voidCode(now));
            for (OrderItemComponent component : item.components()) {
                PartnerProduct product = productMap.get(component.productId());
                if (product != null && product.restockOnRefund()) {
                    inventory.restock(component.productId(), component.quantity(), order.getId());
                }
            }
            points.reverseForItem(item);
            points.returnRedeemedPointsForItem(order, item);
        }
        order.recordRefund(now);
        refunds.save(new Refund(order.getId(), payment.getId(), amount, reason, initiator, actorId, "SUCCEEDED", result.reference(), now));
        orders.saveAndFlush(order);
        catalog.evict();
        events.publishEvent(new OrderRefundedEvent(order.getId(), order.userId(), eligible.stream().map(OrderItem::getId).toList(),
                order.fulfilledAt(), order.status() == OrderStatus.REFUNDED));
    }
}
