package az.buta.catalog.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ComponentRequest(@NotNull UUID productId, @Min(1) @Max(10) int quantity, @DecimalMin("0") BigDecimal negotiatedUnitCost) {
}
