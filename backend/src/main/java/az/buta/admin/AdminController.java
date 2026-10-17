package az.buta.admin;

import az.buta.common.web.PageResponse;
import az.buta.membership.PlanCode;
import az.buta.order.OrderQueryService;
import az.buta.order.RedemptionService;
import az.buta.order.RefundService;
import az.buta.order.domain.OrderStatus;
import az.buta.order.dto.OrderDto;
import az.buta.order.dto.OrderSummaryDto;
import az.buta.order.dto.RefundRequest;
import az.buta.pricing.BusinessSettingsService;
import az.buta.pricing.LoyaltySettings;
import az.buta.pricing.MembershipSettings;
import az.buta.pricing.PricingSettings;
import az.buta.pricing.RecommendationSettings;
import az.buta.pricing.SettingsKey;
import az.buta.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@Validated
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminUserService users;
    private final AdminLoyaltyService loyalty;
    private final AdminInsightsService insights;
    private final BusinessSettingsService settings;
    private final OrderQueryService orders;
    private final RefundService refunds;
    private final RedemptionService redemptions;
    private final GuestPrivacy privacy;

    public AdminController(AdminUserService users, AdminLoyaltyService loyalty, AdminInsightsService insights, BusinessSettingsService settings,
                           OrderQueryService orders, RefundService refunds, RedemptionService redemptions, GuestPrivacy privacy) {
        this.users = users;
        this.loyalty = loyalty;
        this.insights = insights;
        this.settings = settings;
        this.orders = orders;
        this.refunds = refunds;
        this.redemptions = redemptions;
        this.privacy = privacy;
    }

    @GetMapping("/users")
    public PageResponse<AdminDtos.UserView> users(@AuthenticationPrincipal AuthenticatedUser admin,
                                                  @RequestParam(required = false) @Size(max = 100) String query,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return privacy.users(admin, users.search(query, page, size));
    }

    @GetMapping("/users/{id}")
    public AdminDtos.UserView user(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        return privacy.user(admin, users.get(id));
    }

    @PostMapping("/users/{id}/membership")
    public AdminDtos.UserView assignPlus(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                         @Valid @RequestBody AdminDtos.AssignPlusRequest request) {
        return users.assignPlus(admin.userId(), id, request.endsAt(), request.reason());
    }

    @PostMapping("/users/{id}/membership/revoke")
    public AdminDtos.UserView revokePlus(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                         @Valid @RequestBody AdminDtos.ReasonRequest request) {
        return users.revokePlus(admin.userId(), id, request.reason());
    }

    @PostMapping("/users/{id}/suspend")
    public AdminDtos.UserView suspend(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                      @Valid @RequestBody AdminDtos.ReasonRequest request) {
        return users.suspend(admin.userId(), id, request.reason());
    }

    @PostMapping("/users/{id}/reactivate")
    public AdminDtos.UserView reactivate(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        return users.reactivate(admin.userId(), id);
    }

    @PostMapping("/users/{id}/revoke-sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSessions(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        users.revokeSessions(admin.userId(), id);
    }

    @PostMapping("/users/{id}/loyalty-adjustments")
    public AdminDtos.UserView adjustPoints(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                           @Valid @RequestBody AdminDtos.PointsAdjustmentRequest request) {
        return users.adjustPoints(admin.userId(), id, request.points(), request.reason());
    }

    @GetMapping("/loyalty/tasks")
    public List<AdminDtos.TaskView> tasks() {
        return loyalty.tasks();
    }

    @PostMapping("/loyalty/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminDtos.TaskView createTask(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody AdminDtos.TaskRequest request) {
        return loyalty.createTask(admin.userId(), request);
    }

    @PutMapping("/loyalty/tasks/{id}")
    public AdminDtos.TaskView updateTask(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                         @Valid @RequestBody AdminDtos.TaskRequest request) {
        return loyalty.updateTask(admin.userId(), id, request);
    }

    @GetMapping("/loyalty/tiers")
    public List<AdminDtos.TierView> tiers() {
        return loyalty.tiers();
    }

    @PutMapping("/loyalty/tiers")
    public List<AdminDtos.TierView> updateTiers(@AuthenticationPrincipal AuthenticatedUser admin,
                                                @RequestBody @Size(min = 1, max = 10) List<AdminDtos.@Valid TierRequest> request) {
        return loyalty.updateTiers(admin.userId(), request);
    }

    @GetMapping("/referrals")
    public List<AdminDtos.ReferralView> referrals(@AuthenticationPrincipal AuthenticatedUser admin) {
        return privacy.referrals(admin, loyalty.referralsUnderReview());
    }

    @PostMapping("/referrals/{id}/approve")
    public AdminDtos.ReferralView approveReferral(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        return loyalty.approve(admin.userId(), id);
    }

    @PostMapping("/referrals/{id}/reject")
    public AdminDtos.ReferralView rejectReferral(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                                 @Valid @RequestBody AdminDtos.ReasonRequest request) {
        return loyalty.reject(admin.userId(), id, request.reason());
    }

    @GetMapping("/membership/plans")
    public List<AdminDtos.PlanView> plans() {
        return loyalty.plans();
    }

    @PutMapping("/membership/plans/{code}")
    public AdminDtos.PlanView updatePlan(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable PlanCode code,
                                         @Valid @RequestBody AdminDtos.PlanFeaturesRequest request) {
        return loyalty.updatePlan(admin.userId(), code, request);
    }

    @GetMapping("/settings/{key}")
    public Object settings(@PathVariable String key) {
        return settings.raw(SettingsKey.fromKey(key));
    }

    @PutMapping("/settings/pricing")
    public Object updatePricing(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody PricingSettings request) {
        settings.update(SettingsKey.PRICING, request, admin.userId());
        return settings.pricing();
    }

    @PutMapping("/settings/loyalty")
    public Object updateLoyalty(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody LoyaltySettings request) {
        settings.update(SettingsKey.LOYALTY, request, admin.userId());
        return settings.loyalty();
    }

    @PutMapping("/settings/membership")
    public Object updateMembership(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody MembershipSettings request) {
        settings.update(SettingsKey.MEMBERSHIP, request, admin.userId());
        return settings.membership();
    }

    @PutMapping("/settings/recommendation")
    public Object updateRecommendation(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody RecommendationSettings request) {
        settings.update(SettingsKey.RECOMMENDATION, request, admin.userId());
        return settings.recommendation();
    }

    @PostMapping("/settings/pricing/impact-preview")
    public AdminDtos.ImpactPreview pricingImpact(@Valid @RequestBody PricingSettings request) {
        return insights.pricingImpact(request);
    }

    @PostMapping("/settings/loyalty/impact-preview")
    public AdminDtos.ImpactPreview loyaltyImpact(@Valid @RequestBody LoyaltySettings request) {
        return insights.loyaltyImpact(request);
    }

    @GetMapping("/orders")
    public PageResponse<OrderSummaryDto> orders(@RequestParam(required = false) OrderStatus status,
                                                @RequestParam(required = false) @Size(max = 30) String query,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orders.search(status, query, page, size, LocaleContextHolder.getLocale());
    }

    @GetMapping("/orders/{id}")
    public OrderDto order(@PathVariable UUID id) {
        return orders.adminGet(id, LocaleContextHolder.getLocale());
    }

    @PostMapping("/orders/{id}/refunds")
    public OrderDto refund(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody RefundRequest request) {
        return refunds.refundByAdmin(admin.userId(), id, request.orderItemIds(), request.reason(), LocaleContextHolder.getLocale());
    }

    @PostMapping("/redemptions/redeem")
    public RedemptionService.RedemptionResult redeem(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody AdminDtos.RedeemRequest request) {
        return redemptions.redeem(admin.userId(), request.code());
    }

    @GetMapping("/audit-logs")
    public PageResponse<AdminDtos.AuditView> auditLogs(@RequestParam(required = false) @Size(max = 80) String action,
                                                       @RequestParam(required = false) @Size(max = 40) String targetType,
                                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                                       @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return insights.auditLogs(action, targetType, page, size);
    }

    @GetMapping("/recommendations/metrics")
    public List<AdminDtos.VariantMetrics> recommendationMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant end = to == null ? Instant.now() : to;
        return insights.recommendationMetrics(from == null ? end.minus(Duration.ofDays(30)) : from, end);
    }
}
