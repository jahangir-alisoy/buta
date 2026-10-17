package az.buta.catalog.admin;

import az.buta.common.i18n.LocalizedText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CategoryRequest(@Pattern(regexp = "^[a-z0-9-]{2,60}$") String slug, @NotNull @Valid LocalizedText name,
                              @NotNull @Pattern(regexp = "^[a-z-]{2,40}$") String iconKey, @Min(0) @Max(1000) int sortOrder, boolean active) {
}
