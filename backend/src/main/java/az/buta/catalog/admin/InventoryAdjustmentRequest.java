package az.buta.catalog.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InventoryAdjustmentRequest(@Min(-10000) @Max(10000) int delta, @NotBlank @Size(max = 200) String reason) {
}
