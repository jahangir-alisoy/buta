package az.buta.common.text;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public final class SearchKeys {

    private SearchKeys() {
    }

    public static String fold(String input) {
        if (input == null) {
            return "";
        }
        String lower = input.toLowerCase(Locale.ROOT)
                .replace('ə', 'e')
                .replace('ı', 'i')
                .replace('ö', 'o')
                .replace('ü', 'u')
                .replace('ç', 'c')
                .replace('ş', 's')
                .replace('ğ', 'g');
        String withoutMarks = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutMarks.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    public static String build(String... parts) {
        return Arrays.stream(parts)
                .filter(Objects::nonNull)
                .map(SearchKeys::fold)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));
    }
}
