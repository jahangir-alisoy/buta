package az.buta.membership;

import az.buta.audit.AuditService;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.money.Amounts;
import az.buta.common.money.MoneyDto;
import az.buta.payment.PaymentProvider;
import az.buta.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class PlusSubscriptionService {

    private final MembershipRepository memberships;
    private final MembershipPlanRepository plans;
    private final MembershipPurchaseRepository purchases;
    private final MembershipService membershipService;
    private final PaymentProvider paymentProvider;
    private final AuditService audit;
    private final Clock clock;

    public PlusSubscriptionService(MembershipRepository memberships, MembershipPlanRepository plans, MembershipPurchaseRepository purchases,
                                   MembershipService membershipService, PaymentProvider paymentProvider, AuditService audit, Clock clock) {
        this.memberships = memberships;
        this.plans = plans;
        this.purchases = purchases;
        this.membershipService = membershipService;
        this.paymentProvider = paymentProvider;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PlusPlanDto plan(UUID userId) {
        MembershipPlan plan = plusPlan();
        MembershipStatus status = membershipService.current(userId);
        return new PlusPlanDto(MoneyDto.of(plan.price() == null ? BigDecimal.ZERO : plan.price()), plan.billingDuration().toDays(),
                plan.isForSale(), plan.isForSale() && canPurchase(status), status.plan().name(), status.source().name(), status.endsAt());
    }

    @Transactional
    public PlusPlanDto purchase(AuthenticatedUser user) {
        if (!"SANDBOX".equals(paymentProvider.mode())) {
            throw ApiException.notFound();
        }
        grant(user.userId(), purchasablePlan(user), null);
        return plan(user.userId());
    }

    @Transactional(readOnly = true)
    public BigDecimal purchasePrice(AuthenticatedUser user) {
        return Amounts.scale(purchasablePlan(user).price());
    }

    @Transactional
    public boolean completeProviderPurchase(UUID userId, String providerReference) {
        if (purchases.existsByProviderReference(providerReference)) {
            return true;
        }
        MembershipPlan plan = plusPlan();
        if (!plan.isForSale() || !canPurchase(membershipService.current(userId))) {
            return false;
        }
        grant(userId, plan, providerReference);
        return true;
    }

    private MembershipPlan purchasablePlan(AuthenticatedUser user) {
        if (!user.emailVerified()) {
            throw ApiException.of(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        MembershipPlan plan = plusPlan();
        if (!plan.isForSale()) {
            throw ApiException.of(ErrorCode.PLUS_NOT_AVAILABLE);
        }
        if (!canPurchase(membershipService.current(user.userId()))) {
            throw ApiException.of(ErrorCode.ALREADY_PLUS);
        }
        return plan;
    }

    private void grant(UUID userId, MembershipPlan plan, String providerReference) {
        Instant now = clock.instant();
        Membership membership = memberships.findById(userId).orElseGet(() -> Membership.free(userId, now));
        Instant periodStart = membership.isPlusAt(now) && membership.endsAt() != null ? membership.endsAt() : now;
        Instant periodEnd = periodStart.plus(plan.billingDuration());
        membership.grantPlus(MembershipSource.SUBSCRIPTION, periodEnd, null, "PURCHASE", now);
        memberships.save(membership);
        BigDecimal amount = Amounts.scale(plan.price());
        purchases.save(MembershipPurchase.record(userId, PlanCode.PLUS, amount, Amounts.CURRENCY, periodStart, periodEnd,
                paymentProvider.mode(), providerReference, now));
        audit.user(userId, "PLUS_PURCHASED", Map.of("amount", amount, "endsAt", periodEnd.toString()));
    }

    private MembershipPlan plusPlan() {
        return plans.findById(PlanCode.PLUS).orElseThrow(ApiException::notFound);
    }

    private static boolean canPurchase(MembershipStatus status) {
        return status.plan() == PlanCode.FREE || status.source() == MembershipSource.SUBSCRIPTION;
    }
}
