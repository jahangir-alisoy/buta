package az.buta.membership;

import az.buta.audit.AuditService;
import az.buta.common.error.ApiException;
import az.buta.common.money.Amounts;
import az.buta.pricing.BusinessSettingsService;
import az.buta.pricing.MembershipSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MembershipService {

    private final MembershipRepository memberships;
    private final MembershipPlanRepository plans;
    private final BusinessSettingsService settings;
    private final AuditService audit;
    private final Clock clock;

    public MembershipService(MembershipRepository memberships, MembershipPlanRepository plans, BusinessSettingsService settings,
                             AuditService audit, Clock clock) {
        this.memberships = memberships;
        this.plans = plans;
        this.settings = settings;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MembershipStatus current(UUID userId) {
        Instant now = clock.instant();
        return memberships.findById(userId)
                .filter(membership -> membership.isPlusAt(now))
                .map(membership -> new MembershipStatus(PlanCode.PLUS, membership.source(), membership.startsAt(),
                        membership.endsAt(), featuresOf(PlanCode.PLUS)))
                .orElseGet(() -> new MembershipStatus(PlanCode.FREE, MembershipSource.DEFAULT, null, null, featuresOf(PlanCode.FREE)));
    }

    public boolean hasFeature(UUID userId, MembershipFeature feature) {
        return userId != null && current(userId).has(feature);
    }

    @Transactional(readOnly = true)
    public QualifyingSpend qualifyingSpend(UUID userId) {
        MembershipSettings config = settings.membership();
        BigDecimal spent = Amounts.scale(memberships.qualifyingSpendSince(userId,
                clock.instant().minus(Duration.ofDays(config.plusQualifyingWindowDays()))));
        BigDecimal threshold = Amounts.scale(config.plusQualifyingSpendAmount());
        BigDecimal remaining = threshold.subtract(spent).max(BigDecimal.ZERO);
        return new QualifyingSpend(spent, threshold, Amounts.scale(remaining), config.plusQualifyingWindowDays());
    }

    @Transactional
    public void evaluateEarnedPlus(UUID userId) {
        Instant now = clock.instant();
        Membership membership = memberships.findById(userId).orElseGet(() -> Membership.free(userId, now));
        if (membership.isPlusAt(now) && membership.source() == MembershipSource.ADMIN) {
            return;
        }
        QualifyingSpend spend = qualifyingSpend(userId);
        if (spend.spent().compareTo(spend.threshold()) >= 0) {
            boolean wasPlus = membership.isPlusAt(now);
            Instant endsAt = now.plus(Duration.ofDays(settings.membership().earnedPlusDurationDays()));
            if (wasPlus && membership.endsAt() != null && membership.endsAt().isAfter(endsAt)) {
                return;
            }
            membership.grantPlus(MembershipSource.EARNED, endsAt, null, "QUALIFYING_SPEND", now);
            memberships.save(membership);
            if (!wasPlus) {
                audit.system("PLUS_EARNED", "USER", userId, Map.of("qualifyingSpend", spend.spent()));
            }
        } else if (membership.isNew()) {
            memberships.save(membership);
        }
    }

    @Transactional
    public MembershipStatus assignPlus(UUID adminId, UUID userId, Instant endsAt, String reason) {
        Instant now = clock.instant();
        Membership membership = memberships.findById(userId).orElseGet(() -> Membership.free(userId, now));
        membership.grantPlus(MembershipSource.ADMIN, endsAt, adminId, reason, now);
        memberships.save(membership);
        audit.admin(adminId, "PLUS_ASSIGNED", "USER", userId, Map.of("endsAt", String.valueOf(endsAt), "reason", reason));
        return current(userId);
    }

    @Transactional
    public MembershipStatus revokePlus(UUID adminId, UUID userId, String reason) {
        Instant now = clock.instant();
        Membership membership = memberships.findById(userId).orElseThrow(ApiException::notFound);
        membership.downgrade(reason, adminId, now);
        audit.admin(adminId, "PLUS_REVOKED", "USER", userId, Map.of("reason", reason));
        return current(userId);
    }

    @Transactional
    public int expireMemberships() {
        Instant now = clock.instant();
        List<Membership> expired = memberships.findExpiredPlus(now);
        int downgraded = 0;
        for (Membership membership : expired) {
            QualifyingSpend spend = qualifyingSpend(membership.userId());
            if (membership.source() == MembershipSource.EARNED && spend.spent().compareTo(spend.threshold()) >= 0) {
                membership.grantPlus(MembershipSource.EARNED, now.plus(Duration.ofDays(settings.membership().earnedPlusDurationDays())),
                        null, "QUALIFYING_SPEND", now);
            } else {
                membership.downgrade("EXPIRED", null, now);
                audit.system("PLUS_EXPIRED", "USER", membership.userId(), Map.of());
                downgraded++;
            }
        }
        return downgraded;
    }

    @Transactional(readOnly = true)
    public List<MembershipPlan> plans() {
        return plans.findAll();
    }

    @Transactional
    public MembershipPlan updatePlanFeatures(UUID adminId, PlanCode code, Set<MembershipFeature> features) {
        MembershipPlan plan = plans.findById(code).orElseThrow(ApiException::notFound);
        plan.changeFeatures(features, clock.instant());
        audit.admin(adminId, "PLAN_FEATURES_UPDATED", "MEMBERSHIP_PLAN", code, Map.of("features", features));
        return plan;
    }

    private Set<MembershipFeature> featuresOf(PlanCode code) {
        return plans.findById(code).map(MembershipPlan::featureSet).orElse(Set.of());
    }
}
