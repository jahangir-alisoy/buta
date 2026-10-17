package az.buta.admin;

import az.buta.audit.AuditService;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.text.Slugs;
import az.buta.loyalty.ReferralService;
import az.buta.loyalty.domain.LoyaltyTask;
import az.buta.loyalty.domain.LoyaltyTaskRepository;
import az.buta.loyalty.domain.LoyaltyTier;
import az.buta.loyalty.domain.LoyaltyTierRepository;
import az.buta.loyalty.domain.Referral;
import az.buta.membership.MembershipPlan;
import az.buta.membership.MembershipService;
import az.buta.membership.PlanCode;
import az.buta.user.domain.User;
import az.buta.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminLoyaltyService {

    private final LoyaltyTaskRepository tasks;
    private final LoyaltyTierRepository tiers;
    private final ReferralService referrals;
    private final MembershipService membership;
    private final UserRepository users;
    private final AuditService audit;
    private final Clock clock;

    public AdminLoyaltyService(LoyaltyTaskRepository tasks, LoyaltyTierRepository tiers, ReferralService referrals, MembershipService membership,
                               UserRepository users, AuditService audit, Clock clock) {
        this.tasks = tasks;
        this.tiers = tiers;
        this.referrals = referrals;
        this.membership = membership;
        this.users = users;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.TaskView> tasks() {
        return tasks.findAllByOrderByCreatedAtAsc().stream().map(AdminLoyaltyService::view).toList();
    }

    @Transactional
    public AdminDtos.TaskView createTask(UUID adminId, AdminDtos.TaskRequest request) {
        String code = request.code() == null ? Slugs.from(request.title().en()) : request.code();
        if (tasks.existsByCode(code)) {
            throw ApiException.field(ErrorCode.CONFLICT, "code");
        }
        LoyaltyTask task = LoyaltyTask.create(code, request.taskType(), clock.instant());
        apply(task, request);
        tasks.save(task);
        audit.admin(adminId, "TASK_CREATED", "TASK", task.getId(), Map.of("code", code, "rewardPoints", request.rewardPoints()));
        return view(task);
    }

    @Transactional
    public AdminDtos.TaskView updateTask(UUID adminId, UUID id, AdminDtos.TaskRequest request) {
        LoyaltyTask task = tasks.findById(id).orElseThrow(ApiException::notFound);
        if (task.taskType() != request.taskType()) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "taskType");
        }
        apply(task, request);
        audit.admin(adminId, "TASK_UPDATED", "TASK", id, Map.of("rewardPoints", request.rewardPoints(), "active", request.active()));
        return view(task);
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.TierView> tiers() {
        return tiers.findAllByOrderByThresholdPointsAsc().stream().map(AdminLoyaltyService::view).toList();
    }

    @Transactional
    public List<AdminDtos.TierView> updateTiers(UUID adminId, List<AdminDtos.TierRequest> requests) {
        Map<String, AdminDtos.TierRequest> byCode = requests.stream().collect(Collectors.toMap(AdminDtos.TierRequest::code, request -> request));
        List<LoyaltyTier> existing = tiers.findAllByOrderByThresholdPointsAsc();
        if (byCode.size() != existing.size() || existing.stream().anyMatch(tier -> !byCode.containsKey(tier.code()))) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "tiers");
        }
        long distinctThresholds = requests.stream().map(AdminDtos.TierRequest::thresholdPoints).distinct().count();
        boolean hasBase = requests.stream().anyMatch(request -> request.thresholdPoints() == 0);
        if (distinctThresholds != requests.size() || !hasBase) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "thresholdPoints");
        }
        existing.forEach(tier -> {
            AdminDtos.TierRequest request = byCode.get(tier.code());
            tier.update(request.name(), request.thresholdPoints(), request.earnMultiplier(), request.benefits());
        });
        audit.admin(adminId, "TIERS_UPDATED", "LOYALTY_TIERS", "all", Map.of("tiers", requests.stream()
                .map(request -> request.code() + ":" + request.thresholdPoints() + ":" + request.earnMultiplier()).toList()));
        return tiers();
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.ReferralView> referralsUnderReview() {
        return referrals.underReview().stream().map(this::view).toList();
    }

    @Transactional
    public AdminDtos.ReferralView approve(UUID adminId, UUID referralId) {
        return view(referrals.approve(adminId, referralId));
    }

    @Transactional
    public AdminDtos.ReferralView reject(UUID adminId, UUID referralId, String reason) {
        return view(referrals.reject(adminId, referralId, reason));
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.PlanView> plans() {
        return membership.plans().stream().map(AdminLoyaltyService::view).toList();
    }

    @Transactional
    public AdminDtos.PlanView updatePlan(UUID adminId, PlanCode code, AdminDtos.PlanFeaturesRequest request) {
        return view(membership.updatePlanFeatures(adminId, code, request.features()));
    }

    private void apply(LoyaltyTask task, AdminDtos.TaskRequest request) {
        if (request.endsAt() != null && !request.endsAt().isAfter(request.startsAt())) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "endsAt");
        }
        task.update(request.title(), request.description(), request.terms(), request.targetValue(), request.rewardPoints(),
                request.periodType(), request.startsAt(), request.endsAt(), request.active(), request.minOrderAmount(), clock.instant());
    }

    private AdminDtos.ReferralView view(Referral referral) {
        String referrer = users.findById(referral.referrerUserId()).map(User::email).orElse(null);
        String referee = users.findById(referral.refereeUserId()).map(User::email).orElse(null);
        return new AdminDtos.ReferralView(referral.getId(), referral.referrerUserId(), referrer, referral.refereeUserId(), referee,
                referral.status().name(), referral.reviewNote(), referral.createdAt());
    }

    private static AdminDtos.TaskView view(LoyaltyTask task) {
        return new AdminDtos.TaskView(task.getId(), task.code(), task.taskType().name(), task.title(), task.description(), task.terms(),
                task.targetValue(), task.rewardPoints(), task.periodType().name(), task.startsAt(), task.endsAt(), task.active(), task.minOrderAmount());
    }

    private static AdminDtos.TierView view(LoyaltyTier tier) {
        return new AdminDtos.TierView(tier.code(), tier.name(), tier.thresholdPoints(), tier.earnMultiplier(), tier.benefits());
    }

    private static AdminDtos.PlanView view(MembershipPlan plan) {
        return new AdminDtos.PlanView(plan.code().name(), plan.name(), plan.featureSet().stream().map(Enum::name).collect(Collectors.toSet()),
                plan.purchasable());
    }
}
