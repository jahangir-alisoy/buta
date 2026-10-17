package az.buta.auth;

import az.buta.audit.AuditService;
import az.buta.auth.domain.OtpPurpose;
import az.buta.auth.dto.CompleteProfileRequest;
import az.buta.auth.dto.SignInResponse;
import az.buta.common.crypto.IpHasher;
import az.buta.common.crypto.SecureCodes;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.error.RateLimitedException;
import az.buta.security.AuthenticatedUser;
import az.buta.user.UserAccountService;
import az.buta.user.domain.EmailAddresses;
import az.buta.user.domain.User;
import az.buta.user.domain.UserPreferencesRepository;
import az.buta.user.domain.UserPreferences;
import az.buta.user.domain.UserRepository;
import az.buta.user.domain.UserStatus;
import az.buta.user.domain.Usernames;
import az.buta.user.dto.CurrentUserDto;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;

@Service
public class SignInService {

    private final OtpService otpService;
    private final UserRepository users;
    private final UserPreferencesRepository preferences;
    private final UserAccountService accounts;
    private final SessionService sessions;
    private final AdminBootstrap adminBootstrap;
    private final ObjectProvider<ReferralRegistrar> referrals;
    private final AuditService audit;
    private final IpHasher ipHasher;
    private final Clock clock;

    public SignInService(OtpService otpService, UserRepository users, UserPreferencesRepository preferences,
                         UserAccountService accounts, SessionService sessions, AdminBootstrap adminBootstrap,
                         ObjectProvider<ReferralRegistrar> referrals, AuditService audit, IpHasher ipHasher, Clock clock) {
        this.otpService = otpService;
        this.users = users;
        this.preferences = preferences;
        this.accounts = accounts;
        this.sessions = sessions;
        this.adminBootstrap = adminBootstrap;
        this.referrals = referrals;
        this.audit = audit;
        this.ipHasher = ipHasher;
        this.clock = clock;
    }

    public long requestCode(String rawEmail, String ip, Locale locale) {
        return otpService.request(validEmail(rawEmail), OtpPurpose.SIGN_IN, null, ip, locale);
    }

    @Transactional(noRollbackFor = {ApiException.class, RateLimitedException.class})
    public SignInResult verify(String rawEmail, String code, String userAgent, String ip, Locale locale) {
        String email = validEmail(rawEmail);
        otpService.verify(email, OtpPurpose.SIGN_IN, null, code, ip);
        User user = users.findByEmail(email).orElseGet(() -> register(email, ip));
        if (user.status() == UserStatus.SUSPENDED) {
            throw ApiException.of(ErrorCode.OTP_INVALID);
        }
        if (adminBootstrap.synchronize(user)) {
            sessions.revokeAll(user.getId(), "ROLE_CHANGED");
        }
        if (!preferences.existsById(user.getId())) {
            preferences.save(UserPreferences.defaults(user.getId(), locale.getLanguage(), clock.instant()));
        }
        SessionService.IssuedSession session = sessions.open(user, userAgent, ip);
        audit.user(user.getId(), "SIGNED_IN", Map.of());
        String nextStep = user.isActive() ? "DONE" : "COMPLETE_PROFILE";
        return new SignInResult(session.token(), new SignInResponse(nextStep, accounts.currentUser(user.getId())));
    }

    @Transactional
    public CurrentUserDto completeProfile(AuthenticatedUser principal, CompleteProfileRequest request) {
        User user = accounts.require(principal.userId());
        if (user.status() != UserStatus.PROFILE_INCOMPLETE) {
            throw ApiException.of(ErrorCode.FORBIDDEN);
        }
        String username = Usernames.normalize(request.username());
        if (!Usernames.isAllowed(username)) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "username");
        }
        if (users.existsByUsername(username)) {
            throw ApiException.field(ErrorCode.USERNAME_TAKEN, "username");
        }
        user.completeProfile(request.firstName().trim(), request.lastName().trim(), username, clock.instant());
        if (request.referralCode() != null && !request.referralCode().isBlank()) {
            ReferralRegistrar registrar = referrals.getIfAvailable();
            if (registrar != null) {
                registrar.register(user, request.referralCode().trim().toUpperCase(Locale.ROOT));
            }
        }
        audit.user(user.getId(), "PROFILE_COMPLETED", Map.of());
        return accounts.currentUser(user.getId());
    }

    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String rawUsername) {
        String username = Usernames.normalize(rawUsername);
        return Usernames.isAllowed(username) && !users.existsByUsername(username);
    }

    private User register(String email, String ip) {
        String referralCode = SecureCodes.crockford(8);
        while (users.existsByReferralCode(referralCode)) {
            referralCode = SecureCodes.crockford(8);
        }
        User user = User.register(email, referralCode, ipHasher.hash(ip), clock.instant());
        users.save(user);
        audit.user(user.getId(), "REGISTERED", Map.of());
        return user;
    }

    private static String validEmail(String rawEmail) {
        String email = EmailAddresses.normalize(rawEmail);
        if (!EmailAddresses.isValid(email)) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "email");
        }
        return email;
    }

    public record SignInResult(String sessionToken, SignInResponse response) {
    }
}
