package az.buta.auth;

import az.buta.auth.domain.OtpChallenge;
import az.buta.auth.domain.OtpChallengeRepository;
import az.buta.auth.domain.OtpPurpose;
import az.buta.common.config.AppProperties;
import az.buta.common.crypto.Hashing;
import az.buta.common.crypto.IpHasher;
import az.buta.common.crypto.SecureCodes;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.error.RateLimitedException;
import az.buta.notification.EmailMessage;
import az.buta.notification.EmailSender;
import az.buta.notification.EmailTemplateRenderer;
import az.buta.security.RateLimitDecision;
import az.buta.security.RateLimiter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OtpService {

    private static final Duration HOUR = Duration.ofHours(1);

    private final OtpChallengeRepository challenges;
    private final RateLimiter rateLimiter;
    private final EmailSender emailSender;
    private final EmailTemplateRenderer templates;
    private final IpHasher ipHasher;
    private final AppProperties.Otp settings;
    private final Clock clock;

    public OtpService(OtpChallengeRepository challenges, RateLimiter rateLimiter, EmailSender emailSender,
                      EmailTemplateRenderer templates, IpHasher ipHasher, AppProperties properties, Clock clock) {
        this.challenges = challenges;
        this.rateLimiter = rateLimiter;
        this.emailSender = emailSender;
        this.templates = templates;
        this.ipHasher = ipHasher;
        this.settings = properties.otp();
        this.clock = clock;
    }

    @Transactional(noRollbackFor = {ApiException.class, RateLimitedException.class})
    public long request(String email, OtpPurpose purpose, UUID userId, String ip, Locale locale) {
        Instant now = clock.instant();
        enforce(rateLimiter.tryAcquire("otp-request-ip:" + ip, settings.maxPerIpPerHour(), HOUR), ErrorCode.RATE_LIMITED);
        ensureNotLocked(email, now);
        if (challenges.countRequestsSince(email, now.minus(HOUR)) >= settings.maxPerEmailPerHour()) {
            throw new RateLimitedException(ErrorCode.RATE_LIMITED, HOUR);
        }
        challenges.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .map(latest -> latest.createdAt().plus(settings.resendCooldown()))
                .filter(availableAt -> availableAt.isAfter(now))
                .ifPresent(availableAt -> {
                    throw new RateLimitedException(ErrorCode.RATE_LIMITED, Duration.between(now, availableAt));
                });
        challenges.findOpenForUpdate(email, purpose, now).forEach(open -> open.supersede(now));

        String code = SecureCodes.numericCode(6);
        OtpChallenge challenge = OtpChallenge.issue(email, purpose, userId, now, now.plus(settings.ttl()),
                settings.maxAttempts(), ipHasher.hash(ip));
        challenge.assignCodeHash(hash(challenge.getId(), email, code));
        challenges.save(challenge);

        EmailMessage message = templates.otpCode(email, code, settings.ttl().toMinutes(), purpose == OtpPurpose.EMAIL_CHANGE, locale);
        sendAfterCommit(message);
        return settings.resendCooldown().toSeconds();
    }

    @Transactional(noRollbackFor = {ApiException.class, RateLimitedException.class})
    public OtpChallenge verify(String email, OtpPurpose purpose, UUID userId, String code, String ip) {
        Instant now = clock.instant();
        enforce(rateLimiter.tryAcquire("otp-verify-ip:" + ip, settings.maxVerificationsPerIp(), settings.verificationWindow()),
                ErrorCode.RATE_LIMITED);
        ensureNotLocked(email, now);
        List<OtpChallenge> open = challenges.findOpenForUpdate(email, purpose, now);
        if (open.isEmpty()) {
            throw ApiException.of(ErrorCode.OTP_INVALID);
        }
        OtpChallenge challenge = open.getFirst();
        boolean ownerMatches = purpose != OtpPurpose.EMAIL_CHANGE || (userId != null && userId.equals(challenge.userId()));
        if (!ownerMatches || !Hashing.constantTimeEquals(challenge.codeHash(), hash(challenge.getId(), email, code))) {
            if (challenge.registerFailedAttempt(now)) {
                throw new RateLimitedException(ErrorCode.OTP_LOCKED, settings.lockoutDuration());
            }
            throw ApiException.of(ErrorCode.OTP_INVALID);
        }
        challenge.consume(now);
        return challenge;
    }

    @Transactional
    public int purgeOld() {
        return challenges.purgeBefore(clock.instant().minus(Duration.ofDays(7)));
    }

    private void ensureNotLocked(String email, Instant now) {
        Instant windowStart = now.minus(settings.lockoutWindow());
        if (challenges.countLocksSince(email, windowStart) >= settings.lockoutThreshold()) {
            Instant unlockAt = challenges.latestLockSince(email, windowStart).orElse(now).plus(settings.lockoutDuration());
            if (unlockAt.isAfter(now)) {
                throw new RateLimitedException(ErrorCode.OTP_LOCKED, Duration.between(now, unlockAt));
            }
        }
    }

    private String hash(UUID challengeId, String email, String code) {
        return Hashing.hmacSha256Hex(settings.pepper(), challengeId + ":" + email + ":" + code);
    }

    private void sendAfterCommit(EmailMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailSender.send(message);
                }
            });
        } else {
            emailSender.send(message);
        }
    }

    private static void enforce(RateLimitDecision decision, ErrorCode code) {
        if (!decision.allowed()) {
            throw new RateLimitedException(code, decision.retryAfter());
        }
    }
}
