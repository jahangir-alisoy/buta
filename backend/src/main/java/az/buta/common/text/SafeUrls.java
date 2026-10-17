package az.buta.common.text;

import java.net.URI;
import java.util.Locale;

public final class SafeUrls {

    private SafeUrls() {
    }

    public static boolean isSafeImagePath(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (value.startsWith("/images/") && !value.contains("..") && value.matches("[A-Za-z0-9/._-]+")) {
            return true;
        }
        return isHttps(value);
    }

    public static boolean isHttps(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            URI uri = URI.create(value.trim());
            return "https".equals(uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT)) && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
