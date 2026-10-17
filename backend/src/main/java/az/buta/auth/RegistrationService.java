package az.buta.auth;

import az.buta.audit.AuditService;
import az.buta.auth.dto.RegisterRequest;
import az.buta.auth.dto.SignInResponse;
import az.buta.common.crypto.IpHasher;
import az.buta.common.crypto.SecureCodes;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.error.RateLimitedException;
import az.buta.security.RateLimitDecision;
import az.buta.security.RateLimiter;
import az.buta.user.UserAccountService;
import az.buta.user.domain.EmailAddresses;
import az.buta.user.domain.User;
import az.buta.user.domain.UserPreferences;
import az.buta.user.domain.UserPreferencesRepository;
import az.buta.user.domain.UserRepository;
import az.buta.user.domain.Usernames;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final UserPreferencesRepository preferences;
    private final SessionService sessions;
    private final UserAccountService accounts;
    private final ObjectProvider<ReferralRegistrar> referrals;
    private final RateLimiter rateLimiter;
    private final IpHasher ipHasher;
    private final AuditService audit;
    private final Clock clock;

    public RegistrationService(UserRepository users, UserPreferencesRepository preferences, SessionService sessions,
                               UserAccountService accounts, ObjectProvider<ReferralRegistrar> referrals, RateLimiter rateLimiter,
                               IpHasher ipHasher, AuditService audit, Clock clock) {
        this.users = users;
        this.preferences = preferences;
        this.sessions = sessions;
        this.accounts = accounts;
        this.referrals = referrals;
        this.rateLimiter = rateLimiter;
        this.ipHasher = ipHasher;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public SignInService.SignInResult register(RegisterRequest request, String userAgent, String ip, Locale locale) {
        RateLimitDecision decision = rateLimiter.tryAcquire("register-ip:" + ip, 10, Duration.ofHours(1));
        if (!decision.allowed()) {
            throw new RateLimitedException(ErrorCode.RATE_LIMITED, decision.retryAfter());
        }
        String email = EmailAddresses.normalize(request.email());
        if (!EmailAddresses.isValid(email)) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "email");
        }
        if (users.findByEmail(email).isPresent()) {
            throw ApiException.field(ErrorCode.EMAIL_ALREADY_REGISTERED, "email");
        }
        String username = Usernames.normalize(request.username());
        if (!Usernames.isAllowed(username)) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "username");
        }
        if (users.existsByUsername(username)) {
            throw ApiException.field(ErrorCode.USERNAME_TAKEN, "username");
        }
        Instant now = clock.instant();
        User user = User.registerUnverified(email, referralCode(), ipHasher.hash(ip), request.firstName().trim(),
                request.lastName().trim(), username, now);
        users.save(user);
        preferences.save(UserPreferences.defaults(user.getId(), locale.getLanguage(), now));
        if (request.referralCode() != null && !request.referralCode().isBlank()) {
            ReferralRegistrar registrar = referrals.getIfAvailable();
            if (registrar != null) {
                registrar.register(user, request.referralCode().trim().toUpperCase(Locale.ROOT));
            }
        }
        audit.user(user.getId(), "REGISTERED", Map.of("verified", false));
        SessionService.IssuedSession session = sessions.open(user, userAgent, ip);
        return new SignInService.SignInResult(session.token(), new SignInResponse("DONE", accounts.currentUser(user.getId())));
    }

    private String referralCode() {
        String code = SecureCodes.crockford(8);
        while (users.existsByReferralCode(code)) {
            code = SecureCodes.crockford(8);
        }
        return code;
    }
}
