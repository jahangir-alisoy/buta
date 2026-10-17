package az.buta.catalog.admin;

import az.buta.catalog.domain.ActiveStatus;
import az.buta.catalog.domain.CostModel;
import az.buta.catalog.domain.ProductType;
import az.buta.catalog.domain.RedemptionMethod;
import az.buta.common.i18n.LocalizedText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductRequest(@NotNull UUID partnerId, @NotNull UUID categoryId, @NotNull ProductType productType,
                             @NotNull @Valid LocalizedText title, @NotNull @Valid LocalizedText description,
                             @NotNull @Valid LocalizedText terms, @NotNull @Valid LocalizedText redemptionInstructions,
                             @Valid LocalizedText eligibilityNote,
                             @NotNull @DecimalMin("0.01") @DecimalMax("100000") BigDecimal retailPrice, @NotNull CostModel costModel,
                             @DecimalMin("0") @DecimalMax("100000") BigDecimal unitNetCost,
                             @DecimalMin("0") @DecimalMax("100") BigDecimal commissionPercent,
                             @NotNull Instant validFrom, @NotNull Instant validUntil, Instant eventStartsAt, Instant eventEndsAt,
                             @Size(max = 120) String venueName, @Size(max = 200) String venueAddress, @NotBlank @Size(max = 60) String city,
                             @NotNull RedemptionMethod redemptionMethod, boolean refundable, @Min(0) @Max(720) int refundDeadlineHours,
                             boolean restockOnRefund, @NotNull ActiveStatus status, @Min(0) @Max(100000) Integer initialInventory) {
}
