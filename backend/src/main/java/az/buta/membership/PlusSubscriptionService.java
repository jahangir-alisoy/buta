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
        Instant now = clock.instant();
        Membership membership = memberships.findById(user.userId()).orElseGet(() -> Membership.free(user.userId(), now));
        Instant periodStart = membership.isPlusAt(now) && membership.endsAt() != null ? membership.endsAt() : now;
        Instant periodEnd = periodStart.plus(plan.billingDuration());
        membership.grantPlus(MembershipSource.SUBSCRIPTION, periodEnd, null, "PURCHASE", now);
        memberships.save(membership);
        BigDecimal amount = Amounts.scale(plan.price());
        purchases.save(MembershipPurchase.record(user.userId(), PlanCode.PLUS, amount, Amounts.CURRENCY, periodStart, periodEnd,
                paymentProvider.mode(), now));
        audit.user(user.userId(), "PLUS_PURCHASED", Map.of("amount", amount, "endsAt", periodEnd.toString()));
        return plan(user.userId());
    }

    private MembershipPlan plusPlan() {
        return plans.findById(PlanCode.PLUS).orElseThrow(ApiException::notFound);
    }

    private static boolean canPurchase(MembershipStatus status) {
        return status.plan() == PlanCode.FREE || status.source() == MembershipSource.SUBSCRIPTION;
    }
}
