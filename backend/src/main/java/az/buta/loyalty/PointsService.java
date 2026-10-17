package az.buta.loyalty;

import az.buta.catalog.OfferPricingService;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.loyalty.domain.LedgerEntry;
import az.buta.loyalty.domain.LedgerEntryType;
import az.buta.loyalty.domain.LedgerSourceType;
import az.buta.loyalty.domain.LoyaltyAccount;
import az.buta.loyalty.domain.LoyaltyAccountRepository;
import az.buta.loyalty.domain.LoyaltyTier;
import az.buta.loyalty.domain.LoyaltyTierRepository;
import az.buta.membership.MembershipFeature;
import az.buta.membership.MembershipService;
import az.buta.order.domain.Order;
import az.buta.order.domain.OrderItem;
import az.buta.pricing.BusinessSettingsService;
import az.buta.pricing.LoyaltySettings;
import az.buta.pricing.PriceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PointsService {

    private final LedgerService ledger;
    private final LoyaltyAccountRepository accounts;
    private final LoyaltyTierRepository tiers;
    private final MembershipService membership;
    private final BusinessSettingsService settings;
    private final OfferPricingService pricing;

    public PointsService(LedgerService ledger, LoyaltyAccountRepository accounts, LoyaltyTierRepository tiers, MembershipService membership,
                         BusinessSettingsService settings, OfferPricingService pricing) {
        this.ledger = ledger;
        this.accounts = accounts;
        this.tiers = tiers;
        this.membership = membership;
        this.settings = settings;
        this.pricing = pricing;
    }

    @Transactional(readOnly = true)
    public long availablePoints(UUID userId) {
        return accounts.findById(userId).map(LoyaltyAccount::availablePoints).orElse(0L);
    }

    @Transactional(readOnly = true)
    public BigDecimal multiplierFor(UUID userId) {
        return accounts.findById(userId)
                .flatMap(account -> tiers.findById(account.tierCode()))
                .map(LoyaltyTier::earnMultiplier)
                .orElse(BigDecimal.ONE);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void redeemForOrder(UUID userId, UUID orderId, String orderNumber, long points) {
        if (points <= 0) {
            return;
        }
        LoyaltyAccount account = ledger.lockAccount(userId);
        if (account.availablePoints() < points) {
            throw ApiException.of(ErrorCode.INSUFFICIENT_POINTS);
        }
        ledger.post(new LedgerService.PostingRequest(userId, LedgerEntryType.REDEEM, -points, redeemKey(orderId),
                LedgerSourceType.ORDER, orderId, null, "ledger.redeem", Map.of("orderNumber", orderNumber), null));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reverseOrderRedemption(UUID orderId) {
        ledger.reverse(redeemKey(orderId), "ledger.redeemReversal", null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void returnRedeemedPointsForItem(Order order, OrderItem item) {
        if (order.pointsRedeemed() <= 0 || order.pointsDiscountAmount().signum() == 0 || item.pointsDiscountAllocated().signum() == 0) {
            return;
        }
        Optional<LedgerEntry> redeem = ledger.find(redeemKey(order.getId()));
        if (redeem.isEmpty()) {
            return;
        }
        long points = BigDecimal.valueOf(order.pointsRedeemed())
                .multiply(item.pointsDiscountAllocated())
                .divide(order.pointsDiscountAmount(), 0, RoundingMode.HALF_UP)
                .longValue();
        ledger.post(new LedgerService.PostingRequest(order.userId(), LedgerEntryType.REVERSAL, points, "REDEEM_RETURN:" + item.getId(),
                LedgerSourceType.ORDER_ITEM, item.getId(), redeem.get().getId(), "ledger.redeemReturn",
                Map.of("orderNumber", order.orderNumber()), null));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long awardForOrder(Order order) {
        if (!membership.hasFeature(order.userId(), MembershipFeature.LOYALTY_POINTS)) {
            return 0;
        }
        LoyaltySettings loyalty = settings.loyalty();
        BigDecimal multiplier = multiplierFor(order.userId());
        Map<UUID, Integer> bonuses = pricing.context().campaignBonusByOffer();
        long total = 0;
        for (OrderItem item : order.items()) {
            if (item.isRefunded()) {
                continue;
            }
            long points = PriceCalculator.maxPoints(item.cashPaid(), loyalty.pointsPerAzn(), multiplier)
                    + (long) bonuses.getOrDefault(item.offerId(), 0) * item.quantity();
            Optional<LedgerEntry> posted = ledger.post(new LedgerService.PostingRequest(order.userId(), LedgerEntryType.EARN_PURCHASE,
                    points, earnKey(item.getId()), LedgerSourceType.ORDER_ITEM, item.getId(), null, "ledger.earnPurchase",
                    Map.of("orderNumber", order.orderNumber()), null));
            if (posted.isPresent()) {
                item.recordPointsEarned(points);
                total += points;
            }
        }
        return total;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reverseForItem(OrderItem item) {
        ledger.reverse(earnKey(item.getId()), "ledger.earnReversal", null);
    }

    public static String redeemKey(UUID orderId) {
        return "REDEEM:" + orderId;
    }

    public static String earnKey(UUID orderItemId) {
        return "EARN_PURCHASE:" + orderItemId;
    }
}
