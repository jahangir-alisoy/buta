package az.buta.auth;

import az.buta.audit.AuditService;
import az.buta.common.config.AppProperties;
import az.buta.user.domain.Role;
import az.buta.user.domain.RoleSource;
import az.buta.user.domain.User;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.Set;

@Component
public class AdminBootstrap {

    private final Set<String> adminEmails;
    private final AuditService audit;
    private final Clock clock;

    public AdminBootstrap(AppProperties properties, AuditService audit, Clock clock) {
        this.adminEmails = Set.copyOf(properties.adminEmails());
        this.audit = audit;
        this.clock = clock;
    }

    public boolean synchronize(User user) {
        boolean listed = adminEmails.contains(user.email());
        if (listed && !user.hasRole(Role.ADMIN)) {
            user.grantRole(Role.ADMIN, RoleSource.BOOTSTRAP, clock.instant());
            audit.system("ADMIN_ROLE_GRANTED", "USER", user.getId(), Map.of("source", RoleSource.BOOTSTRAP));
            return true;
        }
        if (!listed && user.roleSource(Role.ADMIN) == RoleSource.BOOTSTRAP) {
            user.revokeRole(Role.ADMIN);
            audit.system("ADMIN_ROLE_REVOKED", "USER", user.getId(), Map.of("source", RoleSource.BOOTSTRAP));
            return true;
        }
        return false;
    }
}
