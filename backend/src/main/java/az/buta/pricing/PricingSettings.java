package az.buta.pricing;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PricingSettings(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal paymentFeePercent,
        @NotNull @DecimalMin("0") BigDecimal paymentFeeFixed,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal otherVariableCostPercent,
        @NotNull @DecimalMin("0") BigDecimal otherVariableCostFixed,
        @NotNull @DecimalMin("0") BigDecimal minMarginAmount,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal minMarginPercent,
        @Min(0) @Max(1000) int lowStockThreshold,
        @Min(1) @Max(120) int reservationTtlMinutes) {
}
