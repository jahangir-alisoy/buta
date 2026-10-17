package az.buta.auth;

import az.buta.common.config.AppProperties;
import az.buta.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GuestAccounts {

    private static final Map<GuestKind, String> EMAILS = Map.of(
            GuestKind.CUSTOMER, "kamran.plus@buta.test",
            GuestKind.ADMIN, "admin@buta.test");

    private final boolean enabled;

    public GuestAccounts(AppProperties properties) {
        this.enabled = properties.demoMode();
    }

    public boolean enabled() {
        return enabled;
    }

    public String emailFor(GuestKind kind) {
        return EMAILS.get(kind);
    }

    public boolean isGuest(AuthenticatedUser user) {
        return user != null && EMAILS.containsValue(user.email());
    }
}
