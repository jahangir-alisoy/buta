package az.buta.loyalty;

import az.buta.audit.AuditService;
import az.buta.auth.ReferralRegistrar;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.loyalty.domain.Referral;
import az.buta.loyalty.domain.ReferralRepository;
import az.buta.loyalty.domain.ReferralStatus;
import az.buta.loyalty.task.TaskEngine;
import az.buta.order.domain.Order;
import az.buta.pricing.BusinessSettingsService;
import az.buta.pricing.LoyaltySettings;
import az.buta.user.domain.User;
import az.buta.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ReferralService implements ReferralRegistrar {

    private final ReferralRepository referrals;
    private final UserRepository users;
    private final TaskEngine taskEngine;
    private final BusinessSettingsService settings;
    private final AuditService audit;
    private final az.buta.common.config.AppProperties properties;
    private final Clock clock;

    public ReferralService(ReferralRepository referrals, UserRepository users, TaskEngine taskEngine, BusinessSettingsService settings,
                           AuditService audit, az.buta.common.config.AppProperties properties, Clock clock) {
        this.referrals = referrals;
        this.users = users;
        this.taskEngine = taskEngine;
        this.settings = settings;
        this.audit = audit;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void register(User referee, String referralCode) {
        User referrer = users.findByReferralCode(referralCode)
                .orElseThrow(() -> ApiException.field(ErrorCode.INVALID_REFERRAL_CODE, "referralCode"));
        if (referrer.getId().equals(referee.getId()) || referrer.emailCanonical().equals(referee.emailCanonical())) {
            throw ApiException.field(ErrorCode.INVALID_REFERRAL_CODE, "referralCode");
        }
        if (referrals.existsByRefereeUserId(referee.getId())) {
            return;
        }
        referrals.save(new Referral(referrer.getId(), referee.getId(), referralCode, clock.instant()));
        audit.system("REFERRAL_REGISTERED", "USER", referee.getId(), Map.of("referrerId", referrer.getId()));
    }

    @Transactional
    public void onOrderFulfilled(Order order) {
        Referral referral = referrals.findByRefereeUserId(order.userId()).filter(candidate -> candidate.status() == ReferralStatus.PENDING)
                .orElse(null);
        if (referral == null) {
            return;
        }
        LoyaltySettings.Referral rules = settings.loyalty().referral();
        if (order.totalAmount().compareTo(rules.minFirstOrderAmount()) < 0) {
            return;
        }
        Instant now = clock.instant();
        User referee = users.findById(order.userId()).orElseThrow();
        User referrer = users.findById(referral.referrerUserId()).orElseThrow();
        if (referee.createdAt().plus(Duration.ofDays(rules.maxRefereeAccountAgeDays())).isBefore(now)) {
            referral.reject(null, "ACCOUNT_TOO_OLD", now);
            return;
        }
        Instant monthStart = YearMonth.from(now.atZone(properties.businessTimeZone())).atDay(1)
                .atStartOfDay(properties.businessTimeZone()).toInstant();
        boolean sameNetwork = referee.registrationIpHash() != null && Objects.equals(referee.registrationIpHash(), referrer.registrationIpHash());
        boolean capReached = referrals.countQualifiedSince(referrer.getId(), monthStart) >= rules.maxRewardsPerMonth();
        if (sameNetwork || capReached) {
            referral.holdForReview(order.getId(), sameNetwork ? "SAME_REGISTRATION_NETWORK" : "MONTHLY_CAP_REACHED", now);
            audit.system("REFERRAL_UNDER_REVIEW", "REFERRAL", referral.getId(), Map.of("reason", referral.reviewNote()));
            return;
        }
        referral.qualify(order.getId(), now);
        referrals.flush();
        taskEngine.evaluate(referrer.getId(), now);
    }

    @Transactional
    public void onOrderRefunded(UUID orderId, boolean fullyRefunded) {
        if (!fullyRefunded) {
            return;
        }
        referrals.findByQualifyingOrderId(orderId)
                .filter(referral -> referral.status() == ReferralStatus.QUALIFIED || referral.status() == ReferralStatus.UNDER_REVIEW)
                .ifPresent(referral -> {
                    Instant qualifiedAt = referral.qualifiedAt();
                    referral.reverse(clock.instant());
                    referrals.flush();
                    if (qualifiedAt != null) {
                        taskEngine.evaluate(referral.referrerUserId(), qualifiedAt);
                    }
                });
    }

    @Transactional
    public Referral approve(UUID adminId, UUID referralId) {
        Referral referral = referrals.findById(referralId).filter(candidate -> candidate.status() == ReferralStatus.UNDER_REVIEW)
                .orElseThrow(ApiException::notFound);
        Instant now = clock.instant();
        referral.approve(adminId, now);
        referrals.flush();
        audit.admin(adminId, "REFERRAL_APPROVED", "REFERRAL", referralId, Map.of());
        taskEngine.evaluate(referral.referrerUserId(), now);
        return referral;
    }

    @Transactional
    public Referral reject(UUID adminId, UUID referralId, String note) {
        Referral referral = referrals.findById(referralId).filter(candidate -> candidate.status() == ReferralStatus.UNDER_REVIEW)
                .orElseThrow(ApiException::notFound);
        referral.reject(adminId, note, clock.instant());
        audit.admin(adminId, "REFERRAL_REJECTED", "REFERRAL", referralId, Map.of("note", String.valueOf(note)));
        return referral;
    }

    @Transactional(readOnly = true)
    public List<Referral> forReferrer(UUID referrerId) {
        return referrals.findByReferrerUserIdOrderByCreatedAtDesc(referrerId);
    }

    @Transactional(readOnly = true)
    public List<Referral> underReview() {
        return referrals.findByStatusOrderByCreatedAtDesc(ReferralStatus.UNDER_REVIEW);
    }
}
