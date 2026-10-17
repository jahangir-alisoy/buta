package az.buta.common.i18n;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record LocalizedText(@NotBlank @Size(max = 2000) String az, @NotBlank @Size(max = 2000) String en) {

    public String in(Locale locale) {
        return locale != null && "en".equals(locale.getLanguage()) ? en : az;
    }

    public static LocalizedText of(String az, String en) {
        return new LocalizedText(az, en);
    }
}
