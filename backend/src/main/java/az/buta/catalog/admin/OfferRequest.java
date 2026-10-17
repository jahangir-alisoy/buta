package az.buta.catalog.admin;

import az.buta.catalog.domain.OfferType;
import az.buta.catalog.domain.OfferVisibility;
import az.buta.common.i18n.LocalizedText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OfferRequest(@Pattern(regexp = "^[a-z0-9-]{2,100}$") String slug, @NotNull OfferType offerType,
                           @NotNull UUID primaryCategoryId, @NotNull @Valid LocalizedText title, @NotNull @Valid LocalizedText subtitle,
                           @NotNull @Valid LocalizedText description, @Size(max = 300) String imagePath,
                           @NotNull @DecimalMin("0.01") @DecimalMax("100000") BigDecimal sellingPrice, @NotNull OfferVisibility visibility,
                           @NotNull Instant saleStartsAt, @NotNull Instant saleEndsAt, @Min(1) @Max(10) int maxPerOrder,
                           @Min(0) @Max(100) int businessPriority, boolean featured, @Size(max = 10) List<@Pattern(regexp = "^[a-z0-9-]{1,30}$") String> tags,
                           @NotEmpty @Size(max = 6) List<@Valid ComponentRequest> components) {
}
