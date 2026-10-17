package az.buta.catalog.admin;

import az.buta.catalog.domain.ActiveStatus;
import az.buta.common.i18n.LocalizedText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PartnerRequest(@Pattern(regexp = "^[a-z0-9-]{2,80}$") String slug, @NotBlank @Size(max = 120) String name,
                             @NotNull @Valid LocalizedText description, @NotBlank @Size(max = 60) String city,
                             @Size(max = 200) String address, @Size(max = 300) String websiteUrl, @Size(max = 300) String logoPath,
                             boolean demo, @NotNull ActiveStatus status) {
}
