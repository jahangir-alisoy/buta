package az.buta.user.domain;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class Usernames {

    private static final Pattern FORMAT = Pattern.compile("^[a-z][a-z0-9._]{2,29}$");
    private static final Set<String> RESERVED = Set.of("admin", "administrator", "support", "buta", "root", "system",
            "api", "help", "security", "null", "moderator", "staff");

    private Usernames() {
    }

    public static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isAllowed(String normalized) {
        return FORMAT.matcher(normalized).matches() && !RESERVED.contains(normalized);
    }
}
