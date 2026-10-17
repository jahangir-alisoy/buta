package az.buta.pricing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MembershipSettings(
        @NotNull @DecimalMin("0") BigDecimal plusQualifyingSpendAmount,
        @Min(1) int plusQualifyingWindowDays,
        @Min(1) int earnedPlusDurationDays) {
}
