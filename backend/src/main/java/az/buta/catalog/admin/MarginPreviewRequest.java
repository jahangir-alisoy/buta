package az.buta.catalog.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MarginPreviewRequest(UUID offerId, @NotNull @DecimalMin("0.01") @DecimalMax("100000") BigDecimal sellingPrice,
                                   @NotEmpty @Size(max = 6) List<@Valid ComponentRequest> components) {
}
