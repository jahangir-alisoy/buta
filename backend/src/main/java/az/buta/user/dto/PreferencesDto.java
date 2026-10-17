package az.buta.user.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PreferencesDto(String locale, String theme, boolean personalizationEnabled, boolean personalizationDecided,
                             List<UUID> preferredCategoryIds, BigDecimal budgetMin, BigDecimal budgetMax, String city) {
}
