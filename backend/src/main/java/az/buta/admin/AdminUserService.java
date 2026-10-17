package az.buta.admin;

import az.buta.audit.AuditService;
import az.buta.auth.SessionService;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.web.PageResponse;
import az.buta.loyalty.LedgerService;
import az.buta.loyalty.domain.LedgerEntryType;
import az.buta.loyalty.domain.LedgerSourceType;
import az.buta.loyalty.domain.LoyaltyAccount;
import az.buta.loyalty.domain.LoyaltyAccountRepository;
import az.buta.membership.MembershipService;
import az.buta.membership.MembershipStatus;
import az.buta.user.domain.User;
import az.buta.user.domain.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final UserRepository users;
    private final MembershipService membership;
    private final LoyaltyAccountRepository accounts;
    private final LedgerService ledger;
    private final SessionService sessions;
    private final AuditService audit;
    private final Clock clock;

    public AdminUserService(UserRepository users, MembershipService membership, LoyaltyAccountRepository accounts, LedgerService ledger,
                            SessionService sessions, AuditService audit, Clock clock) {
        this.users = users;
        this.membership = membership;
        this.accounts = accounts;
        this.ledger = ledger;
        this.sessions = sessions;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminDtos.UserView> search(String query, int page, int size) {
        String pattern = "%" + (query == null ? "" : query.strip().toLowerCase(Locale.ROOT)) + "%";
        return PageResponse.of(users.search(pattern, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), this::view);
    }

    @Transactional(readOnly = true)
    public AdminDtos.UserView get(UUID userId) {
        return view(users.findById(userId).orElseThrow(ApiException::notFound));
    }

    @Transactional
    public AdminDtos.UserView assignPlus(UUID adminId, UUID userId, Instant endsAt, String reason) {
        users.findById(userId).orElseThrow(ApiException::notFound);
        if (endsAt != null && !endsAt.isAfter(clock.instant())) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "endsAt");
        }
        membership.assignPlus(adminId, userId, endsAt, reason);
        return get(userId);
    }

    @Transactional
    public AdminDtos.UserView revokePlus(UUID adminId, UUID userId, String reason) {
        membership.revokePlus(adminId, userId, reason);
        return get(userId);
    }

    @Transactional
    public AdminDtos.UserView suspend(UUID adminId, UUID userId, String reason) {
        if (adminId.equals(userId)) {
            throw ApiException.of(ErrorCode.FORBIDDEN);
        }
        User user = users.findById(userId).orElseThrow(ApiException::notFound);
        user.suspend(clock.instant());
        sessions.revokeAll(userId, "SUSPENDED");
        audit.admin(adminId, "USER_SUSPENDED", "USER", userId, Map.of("reason", reason));
        return view(user);
    }

    @Transactional
    public AdminDtos.UserView reactivate(UUID adminId, UUID userId) {
        User user = users.findById(userId).orElseThrow(ApiException::notFound);
        user.reactivate(clock.instant());
        audit.admin(adminId, "USER_REACTIVATED", "USER", userId, Map.of());
        return view(user);
    }

    @Transactional
    public void revokeSessions(UUID adminId, UUID userId) {
        users.findById(userId).orElseThrow(ApiException::notFound);
        sessions.revokeAll(userId, "ADMIN_REVOKED");
        audit.admin(adminId, "USER_SESSIONS_REVOKED", "USER", userId, Map.of());
    }

    @Transactional
    public AdminDtos.UserView adjustPoints(UUID adminId, UUID userId, long points, String reason) {
        users.findById(userId).orElseThrow(ApiException::notFound);
        if (points == 0) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "points");
        }
        ledger.post(new LedgerService.PostingRequest(userId, LedgerEntryType.ADJUSTMENT, points, "ADJUSTMENT:" + UUID.randomUUID(),
                LedgerSourceType.ADMIN, null, null, "ledger.adjustment", Map.of("reason", reason), adminId));
        audit.admin(adminId, "POINTS_ADJUSTED", "USER", userId, Map.of("points", points, "reason", reason));
        return get(userId);
    }

    private AdminDtos.UserView view(User user) {
        MembershipStatus status = membership.current(user.getId());
        Optional<LoyaltyAccount> account = accounts.findById(user.getId());
        return new AdminDtos.UserView(user.getId(), user.email(), user.username(), user.firstName(), user.lastName(), user.status().name(),
                user.roleSet().stream().map(Enum::name).collect(Collectors.toSet()), status.plan().name(), status.source().name(),
                status.endsAt(), account.map(LoyaltyAccount::availablePoints).orElse(0L), account.map(LoyaltyAccount::tierCode).orElse(null),
                account.map(LoyaltyAccount::flaggedForReview).orElse(false), user.createdAt());
    }
}
