package az.buta.auth;

import az.buta.audit.AuditService;
import az.buta.auth.domain.OtpChallenge;
import az.buta.auth.domain.OtpChallengeRepository;
import az.buta.auth.domain.OtpPurpose;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.error.RateLimitedException;
import az.buta.notification.EmailSender;
import az.buta.notification.EmailTemplateRenderer;
import az.buta.security.AuthenticatedUser;
import az.buta.user.UserAccountService;
import az.buta.user.domain.EmailAddresses;
import az.buta.user.domain.User;
import az.buta.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailChangeService {

    private final OtpService otpService;
    private final OtpChallengeRepository challenges;
    private final UserRepository users;
    private final UserAccountService accounts;
    private final SessionService sessions;
    private final EmailSender emailSender;
    private final EmailTemplateRenderer templates;
    private final AuditService audit;
    private final Clock clock;

    public EmailChangeService(OtpService otpService, OtpChallengeRepository challenges, UserRepository users,
                              UserAccountService accounts, SessionService sessions, EmailSender emailSender,
                              EmailTemplateRenderer templates, AuditService audit, Clock clock) {
        this.otpService = otpService;
        this.challenges = challenges;
        this.users = users;
        this.accounts = accounts;
        this.sessions = sessions;
        this.emailSender = emailSender;
        this.templates = templates;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = {ApiException.class, RateLimitedException.class})
    public long request(AuthenticatedUser principal, String rawEmail, String ip, Locale locale) {
        String email = EmailAddresses.normalize(rawEmail);
        if (!EmailAddresses.isValid(email)) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "newEmail");
        }
        User user = accounts.require(principal.userId());
        if (email.equals(user.email())) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "newEmail");
        }
        long resendAfter = otpService.request(email, OtpPurpose.EMAIL_CHANGE, user.getId(), ip, locale);
        audit.user(user.getId(), "EMAIL_CHANGE_REQUESTED", Map.of("newEmail", EmailAddresses.mask(email)));
        return resendAfter;
    }

    @Transactional(noRollbackFor = {ApiException.class, RateLimitedException.class})
    public SessionService.IssuedSession confirm(AuthenticatedUser principal, String code, String userAgent, String ip, Locale locale) {
        User user = accounts.require(principal.userId());
        OtpChallenge latest = challenges.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), OtpPurpose.EMAIL_CHANGE)
                .orElseThrow(() -> ApiException.of(ErrorCode.OTP_INVALID));
        OtpChallenge verified = otpService.verify(latest.email(), OtpPurpose.EMAIL_CHANGE, user.getId(), code, ip);
        String newEmail = verified.email();
        if (users.existsByEmail(newEmail)) {
            throw ApiException.of(ErrorCode.EMAIL_UNAVAILABLE);
        }
        String previousEmail = user.email();
        user.changeEmail(newEmail, clock.instant());
        users.flush();
        sessions.revokeAll(user.getId(), "EMAIL_CHANGED");
        SessionService.IssuedSession session = sessions.open(user, userAgent, ip);
        emailSender.send(templates.emailChangedNotice(previousEmail, EmailAddresses.mask(newEmail), locale));
        audit.user(user.getId(), "EMAIL_CHANGED", Map.of("from", EmailAddresses.mask(previousEmail), "to", EmailAddresses.mask(newEmail)));
        return session;
    }
}
