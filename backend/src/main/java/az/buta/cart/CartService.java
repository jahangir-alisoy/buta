package az.buta.cart;

import az.buta.catalog.OfferCatalog;
import az.buta.catalog.OfferSnapshot;
import az.buta.catalog.OfferViewAssembler;
import az.buta.catalog.Viewer;
import az.buta.catalog.dto.OfferCardDto;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.money.Amounts;
import az.buta.common.money.MoneyDto;
import az.buta.membership.MembershipFeature;
import az.buta.pricing.BusinessSettingsService;
import az.buta.pricing.PriceCalculator;
import az.buta.recommendation.InteractionService;
import az.buta.recommendation.domain.InteractionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CartService {

    static final int MAX_DISTINCT_ITEMS = 20;
    static final int MAX_QUANTITY = 10;

    private final CartRepository carts;
    private final OfferCatalog catalog;
    private final OfferViewAssembler assembler;
    private final InteractionService interactions;
    private final BusinessSettingsService settings;
    private final Clock clock;

    public CartService(CartRepository carts, OfferCatalog catalog, OfferViewAssembler assembler, InteractionService interactions,
                       BusinessSettingsService settings, Clock clock) {
        this.carts = carts;
        this.catalog = catalog;
        this.assembler = assembler;
        this.interactions = interactions;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional
    public CartDto view(Viewer viewer) {
        return toDto(cartOf(viewer.userId()), viewer);
    }

    @Transactional
    public CartDto add(Viewer viewer, AddCartItemRequest request) {
        Cart cart = cartOf(viewer.userId());
        OfferSnapshot snapshot = catalog.byIds(List.of(request.offerId())).get(request.offerId());
        if (snapshot == null) {
            throw ApiException.of(ErrorCode.OFFER_UNAVAILABLE);
        }
        ensurePurchasable(snapshot, viewer);
        Instant now = clock.instant();
        int limit = maxQuantity(snapshot);
        var existing = cart.findByOffer(snapshot.id());
        int desired = existing.map(item -> item.quantity() + request.quantity()).orElse(request.quantity());
        if (desired > limit) {
            throw ApiException.withProperties(ErrorCode.QUANTITY_LIMIT, Map.of("maxQuantity", limit));
        }
        if (existing.isPresent()) {
            existing.get().changeQuantity(desired);
            existing.get().refreshPrice(snapshot.sellingPrice());
            cart.touch(now);
        } else {
            if (cart.items().size() >= MAX_DISTINCT_ITEMS) {
                throw ApiException.of(ErrorCode.CART_LIMIT);
            }
            cart.add(snapshot.id(), desired, snapshot.sellingPrice(), request.recommendationRequestId(), now);
        }
        carts.save(cart);
        interactions.record(viewer.userId(), InteractionType.CART_ADD, snapshot.id(), snapshot.primaryCategory().id(), null,
                request.recommendationRequestId(), null);
        return toDto(cart, viewer);
    }

    @Transactional
    public CartDto updateQuantity(Viewer viewer, UUID itemId, int quantity) {
        Cart cart = cartOf(viewer.userId());
        CartItem item = cart.findItem(itemId).orElseThrow(ApiException::notFound);
        OfferSnapshot snapshot = catalog.byIds(List.of(item.offerId())).get(item.offerId());
        if (snapshot == null) {
            throw ApiException.of(ErrorCode.OFFER_UNAVAILABLE);
        }
        int limit = maxQuantity(snapshot);
        if (quantity > limit) {
            throw ApiException.withProperties(ErrorCode.QUANTITY_LIMIT, Map.of("maxQuantity", limit));
        }
        item.changeQuantity(quantity);
        cart.touch(clock.instant());
        return toDto(cart, viewer);
    }

    @Transactional
    public CartDto remove(Viewer viewer, UUID itemId) {
        Cart cart = cartOf(viewer.userId());
        CartItem item = cart.findItem(itemId).orElseThrow(ApiException::notFound);
        cart.remove(item, clock.instant());
        return toDto(cart, viewer);
    }

    @Transactional
    public Cart cartOf(UUID userId) {
        return carts.findByUserId(userId).orElseGet(() -> carts.save(Cart.open(userId, clock.instant())));
    }

    @Transactional
    public void clear(UUID userId) {
        carts.findByUserId(userId).ifPresent(cart -> cart.clear(clock.instant()));
    }

    @Transactional
    public void restore(UUID userId, List<RestoredItem> items) {
        Cart cart = cartOf(userId);
        Instant now = clock.instant();
        for (RestoredItem restored : items) {
            if (cart.findByOffer(restored.offerId()).isEmpty() && cart.items().size() < MAX_DISTINCT_ITEMS) {
                cart.add(restored.offerId(), Math.min(restored.quantity(), MAX_QUANTITY), restored.unitPrice(), restored.recommendationRequestId(), now);
            }
        }
    }

    public static void ensurePurchasable(OfferSnapshot snapshot, Viewer viewer) {
        String reason = OfferViewAssembler.blockReason(snapshot, viewer);
        if (reason == null) {
            return;
        }
        switch (reason) {
            case "PLUS_REQUIRED" -> throw ApiException.of(ErrorCode.PLUS_REQUIRED);
            case "SOLD_OUT" -> throw ApiException.of(ErrorCode.INSUFFICIENT_INVENTORY);
            default -> throw ApiException.of(ErrorCode.OFFER_UNAVAILABLE);
        }
    }

    private static int maxQuantity(OfferSnapshot snapshot) {
        return Math.max(0, Math.min(Math.min(snapshot.maxPerOrder(), MAX_QUANTITY), snapshot.available()));
    }

    private CartDto toDto(Cart cart, Viewer viewer) {
        Map<UUID, OfferSnapshot> snapshots = catalog.byIds(cart.items().stream().map(CartItem::offerId).toList());
        List<CartDto.Line> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean ready = !cart.items().isEmpty();
        long points = 0;
        boolean earns = viewer.has(MembershipFeature.LOYALTY_POINTS);
        int pointsPerAzn = settings.loyalty().pointsPerAzn();
        for (CartItem item : cart.items()) {
            OfferSnapshot snapshot = snapshots.get(item.offerId());
            String issue = snapshot == null ? "OFFER_UNAVAILABLE" : issueFor(snapshot, viewer, item.quantity());
            OfferCardDto card = snapshot == null ? null : assembler.card(snapshot, viewer);
            BigDecimal unitPrice = snapshot == null ? item.unitPriceAtAdd() : snapshot.sellingPrice();
            BigDecimal lineTotal = Amounts.scale(unitPrice.multiply(BigDecimal.valueOf(item.quantity())));
            if (issue == null) {
                subtotal = subtotal.add(lineTotal);
                if (earns) {
                    points += PriceCalculator.maxPoints(lineTotal, pointsPerAzn, viewer.tierMultiplier())
                            + (long) snapshot.campaignBonusPoints() * item.quantity();
                }
            } else {
                ready = false;
            }
            lines.add(new CartDto.Line(item.getId(), card, item.quantity(), MoneyDto.of(unitPrice), MoneyDto.of(item.unitPriceAtAdd()),
                    unitPrice.compareTo(item.unitPriceAtAdd()) != 0, MoneyDto.of(lineTotal), issue,
                    snapshot == null ? 0 : maxQuantity(snapshot)));
        }
        int count = cart.items().stream().mapToInt(CartItem::quantity).sum();
        return new CartDto(lines, MoneyDto.of(subtotal), count, ready, earns ? points : null);
    }

    private static String issueFor(OfferSnapshot snapshot, Viewer viewer, int quantity) {
        String reason = OfferViewAssembler.blockReason(snapshot, viewer);
        if (reason != null) {
            return reason;
        }
        return quantity > maxQuantity(snapshot) ? "QUANTITY_EXCEEDS_AVAILABLE" : null;
    }

    public record RestoredItem(UUID offerId, int quantity, BigDecimal unitPrice, String recommendationRequestId) {
    }
}
