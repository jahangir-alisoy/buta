package az.buta.order;

import az.buta.catalog.domain.PartnerProduct;
import az.buta.catalog.domain.PartnerProductRepository;
import az.buta.common.money.MoneyDto;
import az.buta.order.domain.Order;
import az.buta.order.domain.OrderItem;
import az.buta.order.domain.OrderStatus;
import az.buta.order.domain.Redemption;
import az.buta.order.domain.RefundRepository;
import az.buta.order.dto.OrderDto;
import az.buta.order.dto.OrderSummaryDto;
import az.buta.payment.PaymentProvider;
import az.buta.payment.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderViewAssembler {

    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final PartnerProductRepository products;
    private final PaymentProvider paymentProvider;
    private final RefundPolicy refundPolicy;
    private final Clock clock;

    public OrderViewAssembler(PaymentRepository payments, RefundRepository refunds, PartnerProductRepository products,
                              PaymentProvider paymentProvider, RefundPolicy refundPolicy, Clock clock) {
        this.payments = payments;
        this.refunds = refunds;
        this.products = products;
        this.paymentProvider = paymentProvider;
        this.refundPolicy = refundPolicy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OrderDto toDto(Order order, Locale locale) {
        return toDto(order, locale, false);
    }

    @Transactional(readOnly = true)
    public OrderDto toDto(Order order, Locale locale, boolean adminView) {
        Instant now = clock.instant();
        Set<UUID> productIds = order.items().stream().flatMap(item -> item.redemptions().stream()).map(Redemption::productId)
                .collect(Collectors.toSet());
        Map<UUID, PartnerProduct> productMap = products.findAllById(productIds).stream()
                .collect(Collectors.toMap(PartnerProduct::getId, Function.identity()));
        boolean revealCodes = order.isPaid() || order.status() == OrderStatus.REFUNDED;
        List<OrderDto.Item> items = order.items().stream().map(item -> {
            String block = refundPolicy.blockReason(order, item, productMap, now, adminView);
            List<OrderDto.Voucher> vouchers = revealCodes ? item.redemptions().stream()
                    .map(redemption -> voucher(redemption, productMap.get(redemption.productId()), locale))
                    .toList() : List.of();
            return new OrderDto.Item(item.getId(), item.offerId(), item.titleSnapshot().in(locale), item.offerType().name(), item.quantity(),
                    MoneyDto.of(item.unitPrice()), MoneyDto.of(item.cashPaid()), item.pointsEarned(), item.isRefunded(), block == null, block,
                    vouchers);
        }).toList();
        OrderDto.PaymentDto payment = payments.findByOrderId(order.getId())
                .map(value -> new OrderDto.PaymentDto(value.getId(), value.provider(), paymentProvider.mode(), value.status().name(),
                        MoneyDto.of(value.amount()), MoneyDto.of(value.refundedAmount())))
                .orElse(null);
        List<OrderDto.RefundDto> refundList = refunds.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(refund -> new OrderDto.RefundDto(MoneyDto.of(refund.amount()), refund.initiatedBy().name(), refund.reason(), refund.createdAt()))
                .toList();
        long earned = order.items().stream().mapToLong(OrderItem::pointsEarned).sum();
        boolean pending = order.status() == OrderStatus.PENDING_PAYMENT;
        return new OrderDto(order.getId(), order.orderNumber(), order.status().name(), MoneyDto.of(order.subtotalAmount()),
                order.pointsRedeemed(), MoneyDto.of(order.pointsDiscountAmount()), MoneyDto.of(order.totalAmount()), earned,
                order.createdAt(), order.expiresAt(), order.fulfilledAt(), order.completedAt(), order.cancelledAt(), order.cancelReason(),
                payment, items, refundList, pending, pending && now.isBefore(order.expiresAt()));
    }

    public OrderSummaryDto summary(Order order, Locale locale) {
        List<OrderItem> items = order.items();
        return new OrderSummaryDto(order.getId(), order.orderNumber(), order.status().name(), MoneyDto.of(order.totalAmount()),
                items.stream().mapToInt(OrderItem::quantity).sum(), items.isEmpty() ? null : items.getFirst().titleSnapshot().in(locale),
                order.createdAt());
    }

    private static OrderDto.Voucher voucher(Redemption redemption, PartnerProduct product, Locale locale) {
        String code = redemption.code();
        String formatted = code.length() == 12 ? code.substring(0, 4) + "-" + code.substring(4, 8) + "-" + code.substring(8) : code;
        return new OrderDto.Voucher(formatted, redemption.status().name(),
                product == null ? null : product.title().in(locale), product == null ? null : product.partner().name(),
                redemption.validFrom(), redemption.validUntil(), product == null ? null : product.eventStartsAt(),
                product == null ? null : product.venueName(), product == null ? null : product.redemptionMethod().name(),
                product == null ? null : product.redemptionInstructions().in(locale));
    }
}
