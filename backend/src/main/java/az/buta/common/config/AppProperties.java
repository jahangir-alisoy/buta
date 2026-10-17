package az.buta.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@ConfigurationProperties("app")
public record AppProperties(
        boolean demoMode,
        String publicBaseUrl,
        List<String> allowedOrigins,
        ZoneId businessTimeZone,
        List<String> adminEmails,
        String ipHashSecret,
        Session session,
        Otp otp,
        Mail mail,
        Payment payment,
        Storage storage) {

    public AppProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream().filter(value -> !value.isBlank()).map(String::trim).toList();
        adminEmails = adminEmails == null ? List.of() : adminEmails.stream().filter(value -> !value.isBlank()).map(value -> value.trim().toLowerCase(Locale.ROOT)).toList();
    }

    public record Session(String cookieName, boolean cookieSecure, Duration idleTimeout, Duration absoluteTimeout,
                          Duration adminIdleTimeout, Duration touchInterval) {
    }

    public record Otp(String pepper, Duration ttl, int maxAttempts, Duration resendCooldown, int maxPerEmailPerHour,
                      int maxPerIpPerHour, int maxVerificationsPerIp, Duration verificationWindow, int lockoutThreshold,
                      Duration lockoutWindow, Duration lockoutDuration) {
    }

    public record Mail(String provider, String from, boolean devMailboxEnabled) {
    }

    public record Payment(String provider) {
    }

    public record Storage(String provider, String localDir, long avatarMaxBytes) {
    }
}
