package az.buta.order;

import az.buta.cart.Cart;
import az.buta.cart.CartItem;
import az.buta.cart.CartService;
import az.buta.catalog.InventoryService;
import az.buta.catalog.OfferCatalog;
import az.buta.catalog.OfferPricingService;
import az.buta.catalog.OfferSnapshot;
import az.buta.catalog.PricingContext;
import az.buta.catalog.Viewer;
import az.buta.catalog.domain.Offer;
import az.buta.catalog.domain.OfferComponent;
import az.buta.catalog.domain.OfferRepository;
import az.buta.common.crypto.Hashing;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.money.Amounts;
import az.buta.common.money.MoneyDto;
import az.buta.loyalty.PointsService;
import az.buta.membership.MembershipFeature;
import az.buta.order.domain.Order;
import az.buta.order.domain.OrderItem;
import az.buta.order.domain.OrderItemComponent;
import az.buta.order.domain.OrderRepository;
import az.buta.order.dto.OrderDto;
import az.buta.order.dto.QuoteDto;
import az.buta.payment.Payment;
import az.buta.payment.PaymentProvider;
import az.buta.payment.PaymentRepository;
import az.buta.pricing.ComponentBreakdown;
import az.buta.pricing.LoyaltySettings;
import az.buta.pricing.PriceBreakdown;
import az.buta.pricing.PriceCalculator;
import az.buta.pricing.PricingSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final CartService carts;
    private final OfferCatalog catalog;
    private final OfferRepository offers;
    private final OfferPricingService pricing;
    private final InventoryService inventory;
    private final PointsService points;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final PaymentProvider paymentProvider;
    private final OrderViewAssembler orderViews;
    private final Clock clock;

    public CheckoutService(CartService carts, OfferCatalog catalog, OfferRepository offers, OfferPricingService pricing,
                           InventoryService inventory, PointsService points, OrderRepository orders, PaymentRepository payments,
                           PaymentProvider paymentProvider, OrderViewAssembler orderViews, Clock clock) {
        this.carts = carts;
        this.catalog = catalog;
        this.offers = offers;
        this.pricing = pricing;
        this.inventory = inventory;
        this.points = points;
        this.orders = orders;
        this.payments = payments;
        this.paymentProvider = paymentProvider;
        this.orderViews = orderViews;
        this.clock = clock;
    }

    @Transactional
    public QuoteDto quote(Viewer viewer, long pointsToRedeem) {
        Draft draft = draft(viewer, pointsToRedeem);
        List<QuoteDto.Line> lines = draft.lines().stream()
                .map(line -> new QuoteDto.Line(line.snapshot().id(), line.snapshot().title().in(viewer.locale()), line.quantity(),
                        MoneyDto.of(line.snapshot().sellingPrice()), MoneyDto.of(line.lineTotal())))
                .toList();
        return new QuoteDto(lines, MoneyDto.of(draft.subtotal()), draft.pointsToRedeem(), MoneyDto.of(draft.pointsDiscount()),
                MoneyDto.of(draft.total()), draft.pointsToEarn(), draft.options(), paymentProvider.mode());
    }

    @Transactional
    public OrderDto place(Viewer viewer, long pointsToRedeem, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 80) {
            throw ApiException.of(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        var existing = orders.findByUserIdAndCheckoutIdempotencyKey(viewer.userId(), idempotencyKey);
        if (existing.isPresent()) {
            Cart cart = carts.cartOf(viewer.userId());
            if (!cart.items().isEmpty() && !fingerprint(cart, pointsToRedeem).equals(existing.get().checkoutFingerprint())) {
                throw ApiException.of(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            return orderViews.toDto(existing.get(), viewer.locale());
        }
        Draft draft = draft(viewer, pointsToRedeem);
        Instant now = clock.instant();
        PricingSettings pricingSettings = draft.context().pricing();
        String orderNumber = "BT-%08d".formatted(orders.nextOrderNumber());
        Order order = Order.place(orderNumber, viewer.userId(), draft.subtotal(), draft.pointsToRedeem(), draft.pointsDiscount(),
                draft.total(), idempotencyKey, draft.fingerprint(), now.plus(Duration.ofMinutes(pricingSettings.reservationTtlMinutes())), now);

        List<BigDecimal> discounts = allocate(draft.pointsDiscount(), draft.lines().stream().map(Line::lineTotal).toList());
        BigDecimal orderFee = Amounts.scale(Amounts.percentOf(draft.total(), pricingSettings.paymentFeePercent()).add(pricingSettings.paymentFeeFixed()));
        List<BigDecimal> fees = allocate(orderFee, draft.lines().stream().map(line -> line.lineTotal()).toList());
        BigDecimal multiplier = viewer.has(MembershipFeature.LOYALTY_POINTS) ? points.multiplierFor(viewer.userId()) : BigDecimal.ZERO;
        LoyaltySettings loyalty = draft.context().loyalty();
        Map<UUID, Integer> reservations = new HashMap<>();
        for (int index = 0; index < draft.lines().size(); index++) {
            Line line = draft.lines().get(index);
            BigDecimal cash = Amounts.scale(line.lineTotal().subtract(discounts.get(index)));
            BigDecimal partnerCost = Amounts.scale(line.breakdown().partnerCostTotal().multiply(BigDecimal.valueOf(line.quantity())));
            long expectedPoints = multiplier.signum() == 0 ? 0
                    : PriceCalculator.maxPoints(cash, loyalty.pointsPerAzn(), multiplier) + (long) line.snapshot().campaignBonusPoints() * line.quantity();
            BigDecimal loyaltyCost = Amounts.scale(BigDecimal.valueOf(expectedPoints).multiply(loyalty.pointValueAzn())
                    .multiply(loyalty.expectedRedemptionPercent()).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
            BigDecimal otherCost = Amounts.scale(Amounts.percentOf(line.lineTotal(), pricingSettings.otherVariableCostPercent())
                    .add(pricingSettings.otherVariableCostFixed().multiply(BigDecimal.valueOf(line.quantity()))));
            BigDecimal margin = Amounts.scale(cash.subtract(partnerCost).subtract(fees.get(index)).subtract(loyaltyCost).subtract(otherCost));
            OrderItem item = new OrderItem(order, line.snapshot().id(), line.snapshot().type(), line.snapshot().primaryCategory().id(),
                    line.snapshot().title(), line.quantity(), line.snapshot().sellingPrice(), line.lineTotal(), discounts.get(index), cash,
                    partnerCost, fees.get(index), loyaltyCost, margin, line.recommendationRequestId(), now);
            for (int componentIndex = 0; componentIndex < line.offer().components().size(); componentIndex++) {
                OfferComponent component = line.offer().components().get(componentIndex);
                ComponentBreakdown breakdown = line.breakdown().components().get(componentIndex);
                int quantity = component.quantity() * line.quantity();
                item.addComponent(new OrderItemComponent(item, component.product().getId(), component.product().partner().getId(), quantity,
                        Amounts.scale(breakdown.allocatedPrice().multiply(BigDecimal.valueOf(line.quantity()))),
                        Amounts.scale(breakdown.partnerCost().multiply(BigDecimal.valueOf(line.quantity())))));
                reservations.merge(component.product().getId(), quantity, Integer::sum);
            }
            order.addItem(item);
        }
        orders.save(order);
        inventory.reserveAll(reservations, order.getId());
        points.redeemForOrder(viewer.userId(), order.getId(), orderNumber, draft.pointsToRedeem());
        payments.save(Payment.initiate(order.getId(), paymentProvider.code(), paymentProvider.createPaymentReference(orderNumber, draft.total()),
                draft.total(), now));
        carts.clear(viewer.userId());
        catalog.evict();
        return orderViews.toDto(order, viewer.locale());
    }

    private Draft draft(Viewer viewer, long pointsToRedeem) {
        Cart cart = carts.cartOf(viewer.userId());
        if (cart.items().isEmpty()) {
            throw ApiException.of(ErrorCode.CART_EMPTY);
        }
        List<UUID> offerIds = cart.items().stream().map(CartItem::offerId).toList();
        Map<UUID, OfferSnapshot> snapshots = catalog.byIds(offerIds);
        Map<UUID, Offer> offerEntities = offers.findAllById(offerIds).stream().collect(Collectors.toMap(Offer::getId, offer -> offer));
        PricingContext context = pricing.context();
        List<Line> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.items()) {
            OfferSnapshot snapshot = snapshots.get(cartItem.offerId());
            if (snapshot == null) {
                throw ApiException.of(ErrorCode.OFFER_UNAVAILABLE);
            }
            CartService.ensurePurchasable(snapshot, viewer);
            int limit = Math.min(Math.min(snapshot.maxPerOrder(), 10), snapshot.available());
            if (cartItem.quantity() > limit) {
                throw ApiException.withProperties(ErrorCode.QUANTITY_LIMIT, Map.of("offerId", snapshot.id().toString(), "maxQuantity", limit));
            }
            Offer offer = offerEntities.get(snapshot.id());
            PriceBreakdown breakdown = pricing.breakdown(offer, context);
            if (!breakdown.sellable()) {
                throw ApiException.of(ErrorCode.OFFER_NOT_SELLABLE);
            }
            BigDecimal lineTotal = Amounts.scale(snapshot.sellingPrice().multiply(BigDecimal.valueOf(cartItem.quantity())));
            subtotal = subtotal.add(lineTotal);
            lines.add(new Line(snapshot, offer, breakdown, cartItem.quantity(), lineTotal, cartItem.recommendationRequestId()));
        }
        subtotal = Amounts.scale(subtotal);
        LoyaltySettings loyalty = context.loyalty();
        boolean redemptionAllowed = viewer.has(MembershipFeature.POINTS_REDEMPTION);
        long available = viewer.isAuthenticated() ? points.availablePoints(viewer.userId()) : 0;
        BigDecimal maxDiscount = Amounts.percentOf(subtotal, loyalty.maxPointsDiscountPercent());
        long maxByDiscount = loyalty.pointValueAzn().signum() == 0 ? 0
                : maxDiscount.divide(loyalty.pointValueAzn(), 0, RoundingMode.FLOOR).longValue();
        long maxRedeemable = redemptionAllowed ? floorToStep(Math.min(Math.max(available, 0), maxByDiscount), loyalty.redemptionStepPoints()) : 0;
        if (maxRedeemable < loyalty.minRedeemPoints()) {
            maxRedeemable = 0;
        }
        if (pointsToRedeem > 0) {
            if (!redemptionAllowed) {
                throw ApiException.of(ErrorCode.POINTS_REDEMPTION_NOT_ALLOWED);
            }
            if (pointsToRedeem % loyalty.redemptionStepPoints() != 0 || pointsToRedeem < loyalty.minRedeemPoints() || pointsToRedeem > maxByDiscount) {
                throw ApiException.of(ErrorCode.POINTS_REDEMPTION_LIMIT);
            }
            if (pointsToRedeem > available) {
                throw ApiException.of(ErrorCode.INSUFFICIENT_POINTS);
            }
        }
        BigDecimal pointsDiscount = Amounts.scale(loyalty.pointValueAzn().multiply(BigDecimal.valueOf(pointsToRedeem)));
        BigDecimal total = Amounts.scale(subtotal.subtract(pointsDiscount));
        Long pointsToEarn = null;
        if (viewer.has(MembershipFeature.LOYALTY_POINTS)) {
            BigDecimal multiplier = points.multiplierFor(viewer.userId());
            List<BigDecimal> discounts = allocate(pointsDiscount, lines.stream().map(Line::lineTotal).toList());
            long earn = 0;
            for (int index = 0; index < lines.size(); index++) {
                Line line = lines.get(index);
                earn += PriceCalculator.maxPoints(line.lineTotal().subtract(discounts.get(index)), loyalty.pointsPerAzn(), multiplier)
                        + (long) line.snapshot().campaignBonusPoints() * line.quantity();
            }
            pointsToEarn = earn;
        }
        String fingerprint = fingerprint(cart, pointsToRedeem);
        QuoteDto.PointsOptions options = new QuoteDto.PointsOptions(redemptionAllowed, available, maxRedeemable,
                loyalty.redemptionStepPoints(), loyalty.minRedeemPoints(), loyalty.pointValueAzn().toPlainString());
        return new Draft(lines, subtotal, pointsToRedeem, pointsDiscount, total, pointsToEarn, options, fingerprint, context);
    }

    private static String fingerprint(Cart cart, long pointsToRedeem) {
        return Hashing.sha256Hex(cart.items().stream()
                .sorted(Comparator.comparing(item -> item.offerId().toString()))
                .map(item -> item.offerId() + ":" + item.quantity())
                .collect(Collectors.joining(",")) + "|" + pointsToRedeem);
    }

    static List<BigDecimal> allocate(BigDecimal amount, List<BigDecimal> weights) {
        List<BigDecimal> shares = new ArrayList<>();
        BigDecimal totalWeight = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < weights.size(); index++) {
            BigDecimal share;
            if (index == weights.size() - 1) {
                share = Amounts.scale(amount.subtract(allocated));
            } else if (totalWeight.signum() == 0) {
                share = Amounts.zero();
            } else {
                share = Amounts.scale(amount.multiply(weights.get(index)).divide(totalWeight, 10, RoundingMode.HALF_UP));
            }
            shares.add(share);
            allocated = allocated.add(share);
        }
        return shares;
    }

    private static long floorToStep(long value, int step) {
        return step <= 0 ? value : (value / step) * step;
    }

    private record Line(OfferSnapshot snapshot, Offer offer, PriceBreakdown breakdown, int quantity, BigDecimal lineTotal,
                        String recommendationRequestId) {
    }

    private record Draft(List<Line> lines, BigDecimal subtotal, long pointsToRedeem, BigDecimal pointsDiscount, BigDecimal total,
                         Long pointsToEarn, QuoteDto.PointsOptions options, String fingerprint, PricingContext context) {
    }
}
