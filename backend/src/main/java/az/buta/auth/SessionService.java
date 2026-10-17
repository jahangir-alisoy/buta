package az.buta.auth;

import az.buta.auth.domain.UserSession;
import az.buta.auth.domain.UserSessionRepository;
import az.buta.common.config.AppProperties;
import az.buta.common.crypto.Hashing;
import az.buta.common.crypto.IpHasher;
import az.buta.common.crypto.SecureCodes;
import az.buta.security.AuthenticatedUser;
import az.buta.user.domain.Role;
import az.buta.user.domain.User;
import az.buta.user.domain.UserRepository;
import az.buta.user.domain.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    private final UserSessionRepository sessions;
    private final UserRepository users;
    private final AppProperties.Session settings;
    private final IpHasher ipHasher;
    private final Clock clock;

    public SessionService(UserSessionRepository sessions, UserRepository users, AppProperties properties, IpHasher ipHasher, Clock clock) {
        this.sessions = sessions;
        this.users = users;
        this.settings = properties.session();
        this.ipHasher = ipHasher;
        this.clock = clock;
    }

    @Transactional
    public IssuedSession open(User user, String userAgent, String ip) {
        String token = SecureCodes.token(32);
        UserSession session = UserSession.open(user.getId(), Hashing.sha256Hex(token), clock.instant(),
                idleTimeoutFor(user), settings.absoluteTimeout(), userAgent, ipHasher.hash(ip));
        sessions.save(session);
        return new IssuedSession(token, session.getId());
    }

    @Transactional
    public Optional<AuthenticatedUser> authenticate(String token) {
        Instant now = clock.instant();
        return sessions.findByTokenHash(Hashing.sha256Hex(token))
                .filter(session -> session.isUsable(now))
                .flatMap(session -> users.findById(session.userId())
                        .filter(user -> user.status() != UserStatus.SUSPENDED)
                        .map(user -> {
                            session.touch(now, idleTimeoutFor(user), settings.touchInterval());
                            return new AuthenticatedUser(user.getId(), session.getId(), user.email(),
                                    user.isEmailVerified(), user.status(), user.roleSet());
                        }));
    }

    @Transactional
    public void revoke(UUID sessionId, String reason) {
        sessions.findById(sessionId).ifPresent(session -> session.revoke(clock.instant(), reason));
    }

    @Transactional
    public void revokeAll(UUID userId, String reason) {
        sessions.revokeAllForUser(userId, clock.instant(), reason);
    }

    @Transactional
    public void revokeOthers(UUID userId, UUID keepSessionId, String reason) {
        sessions.revokeOthers(userId, keepSessionId, clock.instant(), reason);
    }

    @Transactional
    public int purgeExpired() {
        return sessions.purgeBefore(clock.instant().minus(Duration.ofDays(30)));
    }

    private Duration idleTimeoutFor(User user) {
        return user.hasRole(Role.ADMIN) ? settings.adminIdleTimeout() : settings.idleTimeout();
    }

    public record IssuedSession(String token, UUID sessionId) {
    }
}
