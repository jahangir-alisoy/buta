package az.buta.common.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class StartupGuards implements InitializingBean {

    private static final int MIN_SECRET_BYTES = 32;

    private final AppProperties properties;
    private final Environment environment;

    public StartupGuards(AppProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> problems = new ArrayList<>();
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        if (properties.mail().devMailboxEnabled() && !environment.acceptsProfiles(Profiles.of("dev"))) {
            problems.add("APP_DEV_MAILBOX_ENABLED is only allowed with the dev profile");
        }
        if (production) {
            if (tooShort(properties.otp().pepper())) {
                problems.add("APP_OTP_PEPPER must be at least 32 bytes");
            }
            if (tooShort(properties.ipHashSecret())) {
                problems.add("APP_IP_HASH_SECRET must be at least 32 bytes");
            }
            if (properties.otp().pepper() != null && properties.otp().pepper().startsWith("dev-only")) {
                problems.add("APP_OTP_PEPPER must not use the development value");
            }
            if ("in-memory".equals(properties.mail().provider())) {
                problems.add("The in-memory email provider is not allowed in production");
            }
            if (isBlank(environment.getProperty("spring.mail.username")) || isBlank(environment.getProperty("spring.mail.password"))) {
                problems.add("MAIL_USERNAME and MAIL_APP_PASSWORD are required in production");
            }
            if ("sandbox".equals(properties.payment().provider()) && !properties.demoMode()) {
                problems.add("Sandbox payments require APP_DEMO_MODE=true so the demo is clearly labeled");
            }
            if (!properties.session().cookieSecure()) {
                problems.add("APP_SESSION_COOKIE_SECURE must be true in production");
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Unsafe configuration: " + String.join("; ", problems));
        }
    }

    private static boolean tooShort(String secret) {
        return secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
