package az.buta.admin;

import az.buta.common.i18n.LocalizedText;
import az.buta.user.domain.EmailAddresses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record UserView(UUID id, String email, String username, String firstName, String lastName, String status, Set<String> roles,
                           String plan, String planSource, Instant planEndsAt, long availablePoints, String tier, boolean flaggedForReview,
                           Instant createdAt) {

        public UserView withMaskedEmail() {
            return new UserView(id, EmailAddresses.mask(email), username, firstName, lastName, status, roles, plan, planSource, planEndsAt,
                    availablePoints, tier, flaggedForReview, createdAt);
        }
    }

    public record AssignPlusRequest(Instant endsAt, @NotBlank @Size(max = 200) String reason) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 200) String reason) {
    }

    public record PointsAdjustmentRequest(@Min(-100000) @Max(100000) long points, @NotBlank @Size(max = 200) String reason) {
    }

    public record TaskRequest(@Pattern(regexp = "^[a-z0-9-]{2,60}$") String code, @NotNull az.buta.loyalty.domain.TaskType taskType,
                              @NotNull @Valid LocalizedText title, @NotNull @Valid LocalizedText description, @NotNull @Valid LocalizedText terms,
                              @Min(1) @Max(100) int targetValue, @Min(1) @Max(100000) long rewardPoints,
                              @NotNull az.buta.loyalty.domain.TaskPeriod periodType, @NotNull Instant startsAt, Instant endsAt, boolean active,
                              @NotNull @DecimalMin("0") @DecimalMax("10000") BigDecimal minOrderAmount) {
    }

    public record TaskView(UUID id, String code, String taskType, LocalizedText title, LocalizedText description, LocalizedText terms,
                           int targetValue, long rewardPoints, String periodType, Instant startsAt, Instant endsAt, boolean active,
                           BigDecimal minOrderAmount) {
    }

    public record TierRequest(@NotBlank String code, @NotNull @Valid LocalizedText name, @Min(0) @Max(10_000_000) long thresholdPoints,
                              @NotNull @DecimalMin("1.00") @DecimalMax("5.00") BigDecimal earnMultiplier,
                              @NotNull Map<String, List<@Size(max = 120) String>> benefits) {
    }

    public record TierView(String code, LocalizedText name, long thresholdPoints, BigDecimal earnMultiplier, Map<String, List<String>> benefits) {
    }

    public record ReferralView(UUID id, UUID referrerId, String referrerEmail, UUID refereeId, String refereeEmail, String status,
                               String reviewNote, Instant createdAt) {

        public ReferralView withMaskedEmails() {
            return new ReferralView(id, referrerId, EmailAddresses.mask(referrerEmail), refereeId, EmailAddresses.mask(refereeEmail), status,
                    reviewNote, createdAt);
        }
    }

    public record PlanView(String code, LocalizedText name, Set<String> features, boolean purchasable) {
    }

    public record PlanFeaturesRequest(@NotNull Set<az.buta.membership.MembershipFeature> features) {
    }

    public record RedeemRequest(@NotBlank @Size(max = 20) String code) {
    }

    public record AuditView(UUID id, UUID actorUserId, String actorType, String action, String targetType, String targetId,
                            Map<String, Object> details, String requestId, Instant createdAt) {
    }

    public record ImpactPreview(int affectedOffers, List<String> failingOffers) {
    }

    public record VariantMetrics(String variant, long impressions, long clicks, long cartAdds, long purchases, BigDecimal revenue,
                                 BigDecimal contributionMargin, String clickThroughRatePercent, String conversionRatePercent) {
    }
}
