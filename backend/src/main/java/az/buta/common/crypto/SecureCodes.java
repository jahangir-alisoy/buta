package az.buta.common.crypto;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecureCodes {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private SecureCodes() {
    }

    public static String numericCode(int digits) {
        int bound = (int) Math.pow(10, digits);
        return String.format("%0" + digits + "d", RANDOM.nextInt(bound));
    }

    public static String token(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    public static String crockford(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(CROCKFORD[RANDOM.nextInt(CROCKFORD.length)]);
        }
        return builder.toString();
    }
}
