package az.buta.security;

import az.buta.user.domain.Role;
import az.buta.user.domain.UserStatus;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, UUID sessionId, UserStatus status, Set<Role> roles) {

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }
}
