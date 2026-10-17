package az.buta.pricing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoyaltySettings(
        @Min(0) @Max(100) int pointsPerAzn,
        @NotNull @DecimalMin("0") BigDecimal pointValueAzn,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal expectedRedemptionPercent,
        @Min(1) int redemptionStepPoints,
        @Min(1) int minRedeemPoints,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal maxPointsDiscountPercent,
        @NotNull AwardTrigger awardTrigger,
        @Min(0) @Max(120) int tierWindowMonths,
        @NotNull @Valid Referral referral) {

    public record Referral(@NotNull @DecimalMin("0") BigDecimal minFirstOrderAmount,
                           @Min(0) int maxRewardsPerMonth,
                           @Min(1) int maxRefereeAccountAgeDays) {
    }
}
