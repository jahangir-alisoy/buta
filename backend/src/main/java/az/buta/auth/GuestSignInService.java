package az.buta.auth;

import az.buta.audit.AuditService;
import az.buta.auth.dto.SignInResponse;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.error.RateLimitedException;
import az.buta.security.RateLimitDecision;
import az.buta.security.RateLimiter;
import az.buta.user.UserAccountService;
import az.buta.user.domain.User;
import az.buta.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;

@Service
public class GuestSignInService {

    private final GuestAccounts guests;
    private final UserRepository users;
    private final SessionService sessions;
    private final UserAccountService accounts;
    private final RateLimiter rateLimiter;
    private final AuditService audit;

    public GuestSignInService(GuestAccounts guests, UserRepository users, SessionService sessions, UserAccountService accounts,
                              RateLimiter rateLimiter, AuditService audit) {
        this.guests = guests;
        this.users = users;
        this.sessions = sessions;
        this.accounts = accounts;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
    }

    @Transactional
    public SignInService.SignInResult signIn(GuestKind kind, String userAgent, String ip) {
        if (!guests.enabled()) {
            throw ApiException.notFound();
        }
        RateLimitDecision decision = rateLimiter.tryAcquire("guest-ip:" + ip, 30, Duration.ofHours(1));
        if (!decision.allowed()) {
            throw new RateLimitedException(ErrorCode.RATE_LIMITED, decision.retryAfter());
        }
        User user = users.findByEmail(guests.emailFor(kind)).filter(User::isActive).orElseThrow(ApiException::notFound);
        SessionService.IssuedSession session = sessions.open(user, userAgent, ip);
        audit.user(user.getId(), "GUEST_SIGNED_IN", Map.of("kind", kind.name()));
        return new SignInService.SignInResult(session.token(), new SignInResponse("DONE", accounts.currentUser(user.getId())));
    }
}
