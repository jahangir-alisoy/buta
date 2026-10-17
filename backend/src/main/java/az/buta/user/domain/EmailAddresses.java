package az.buta.user.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailAddresses {

    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private EmailAddresses() {
    }

    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String normalized) {
        return normalized.length() <= 254 && FORMAT.matcher(normalized).matches();
    }

    public static String canonical(String normalized) {
        int at = normalized.lastIndexOf('@');
        if (at < 0) {
            return normalized;
        }
        String local = normalized.substring(0, at);
        String domain = normalized.substring(at + 1);
        int plus = local.indexOf('+');
        if (plus >= 0) {
            local = local.substring(0, plus);
        }
        if (domain.equals("gmail.com") || domain.equals("googlemail.com")) {
            local = local.replace(".", "");
            domain = "gmail.com";
        }
        return local + "@" + domain;
    }

    public static String mask(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
