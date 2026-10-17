package az.buta.user.dto;

import az.buta.user.domain.Theme;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdatePreferencesRequest(
        @NotNull @Pattern(regexp = "az|en") String locale,
        @NotNull Theme theme,
        Boolean personalizationEnabled,
        @NotNull @Size(max = 9) List<UUID> preferredCategoryIds,
        @DecimalMin("0") @DecimalMax("10000") BigDecimal budgetMin,
        @DecimalMin("0") @DecimalMax("10000") BigDecimal budgetMax,
        @Size(max = 60) String city) {
}
