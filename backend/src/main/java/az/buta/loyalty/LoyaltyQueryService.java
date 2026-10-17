package az.buta.loyalty;

import az.buta.common.config.AppProperties;
import az.buta.common.money.MoneyDto;
import az.buta.common.web.PageResponse;
import az.buta.loyalty.domain.LedgerEntryRepository;
import az.buta.loyalty.domain.LoyaltyAccount;
import az.buta.loyalty.domain.LoyaltyAccountRepository;
import az.buta.loyalty.domain.LoyaltyTask;
import az.buta.loyalty.domain.LoyaltyTaskRepository;
import az.buta.loyalty.domain.LoyaltyTier;
import az.buta.loyalty.domain.LoyaltyTierRepository;
import az.buta.loyalty.domain.TaskProgress;
import az.buta.loyalty.domain.TaskProgressRepository;
import az.buta.loyalty.dto.LedgerEntryDto;
import az.buta.loyalty.dto.LoyaltySummaryDto;
import az.buta.loyalty.dto.ReferralInfoDto;
import az.buta.loyalty.dto.TaskDto;
import az.buta.loyalty.task.TaskWindow;
import az.buta.loyalty.task.TaskWindows;
import az.buta.membership.MembershipFeature;
import az.buta.membership.MembershipService;
import az.buta.membership.MembershipStatus;
import az.buta.membership.QualifyingSpend;
import az.buta.user.domain.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LoyaltyQueryService {

    private final LoyaltyAccountRepository accounts;
    private final LoyaltyTierRepository tiers;
    private final LedgerEntryRepository ledger;
    private final LoyaltyTaskRepository tasks;
    private final TaskProgressRepository progress;
    private final MembershipService membership;
    private final ReferralService referrals;
    private final UserRepository users;
    private final AppProperties properties;
    private final Clock clock;

    public LoyaltyQueryService(LoyaltyAccountRepository accounts, LoyaltyTierRepository tiers, LedgerEntryRepository ledger,
                               LoyaltyTaskRepository tasks, TaskProgressRepository progress, MembershipService membership,
                               ReferralService referrals, UserRepository users, AppProperties properties, Clock clock) {
        this.accounts = accounts;
        this.tiers = tiers;
        this.ledger = ledger;
        this.tasks = tasks;
        this.progress = progress;
        this.membership = membership;
        this.referrals = referrals;
        this.users = users;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LoyaltySummaryDto summary(UUID userId, Locale locale) {
        MembershipStatus status = membership.current(userId);
        Optional<LoyaltyAccount> account = accounts.findById(userId);
        List<LoyaltyTier> ordered = tiers.findAllByOrderByThresholdPointsAsc();
        long qualifying = account.map(LoyaltyAccount::tierQualifyingPoints).orElse(0L);
        LoyaltyTier current = ordered.getFirst();
        LoyaltyTier next = null;
        for (LoyaltyTier tier : ordered) {
            if (qualifying >= tier.thresholdPoints()) {
                current = tier;
            } else if (next == null) {
                next = tier;
            }
        }
        int percent = next == null ? 100 : (int) Math.min(100, Math.max(0,
                (qualifying - current.thresholdPoints()) * 100 / Math.max(1, next.thresholdPoints() - current.thresholdPoints())));
        QualifyingSpend spend = membership.qualifyingSpend(userId);
        String language = locale.getLanguage();
        return new LoyaltySummaryDto(status.plan().name(), status.has(MembershipFeature.LOYALTY_POINTS),
                account.map(LoyaltyAccount::availablePoints).orElse(0L), account.map(LoyaltyAccount::lifetimeEarnedPoints).orElse(0L),
                account.map(LoyaltyAccount::lifetimeRedeemedPoints).orElse(0L), qualifying, tier(current, locale),
                next == null ? null : new LoyaltySummaryDto.NextTier(next.code(), next.name().in(locale), next.thresholdPoints(),
                        next.thresholdPoints() - qualifying),
                percent,
                new LoyaltySummaryDto.QualifyingSpend(MoneyDto.of(spend.spent()), MoneyDto.of(spend.threshold()),
                        MoneyDto.of(spend.remaining()), spend.windowDays()),
                account.map(value -> value.availablePoints() < 0).orElse(false), status.endsAt(), status.source().name(),
                ordered.stream().map(tier -> tier(tier, locale)).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<LedgerEntryDto> ledger(UUID userId, int page, int size) {
        return PageResponse.of(ledger.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)),
                entry -> new LedgerEntryDto(entry.getId(), entry.entryType().name(), entry.points(), entry.descriptionKey(),
                        entry.descriptionParams(), entry.createdAt()));
    }

    @Transactional(readOnly = true)
    public List<TaskDto> tasks(UUID userId, Locale locale) {
        Instant now = clock.instant();
        Map<String, TaskProgress> progressByKey = progress.findByUserId(userId).stream()
                .collect(Collectors.toMap(row -> row.taskId() + ":" + row.periodKey(), Function.identity(), (left, right) -> left));
        return tasks.findByActiveTrue().stream()
                .filter(task -> task.isRunningAt(now))
                .map(task -> {
                    TaskWindow window = TaskWindows.forTask(task, now, properties.businessTimeZone());
                    TaskProgress row = progressByKey.get(task.getId() + ":" + window.periodKey());
                    return toDto(task, window.periodKey(), row, locale);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ReferralInfoDto referral(UUID userId) {
        String code = users.findById(userId).map(user -> user.referralCode()).orElse(null);
        return new ReferralInfoDto(code, referrals.forReferrer(userId).stream()
                .map(referral -> new ReferralInfoDto.Entry(referral.status().name(), referral.createdAt(), referral.qualifiedAt()))
                .toList());
    }

    private static TaskDto toDto(LoyaltyTask task, String periodKey, TaskProgress row, Locale locale) {
        return new TaskDto(task.getId(), task.code(), task.taskType().name(), task.title().in(locale), task.description().in(locale),
                task.terms().in(locale), row == null ? 0 : Math.min(row.progressValue(), task.targetValue()), task.targetValue(),
                task.rewardPoints(), task.periodType().name(), periodKey, row == null ? "IN_PROGRESS" : row.status(),
                row != null && row.awarded(), row == null ? null : row.completedAt(), task.endsAt());
    }

    private static LoyaltySummaryDto.Tier tier(LoyaltyTier tier, Locale locale) {
        return new LoyaltySummaryDto.Tier(tier.code(), tier.name().in(locale), tier.thresholdPoints(), tier.earnMultiplier().toPlainString(),
                tier.benefits(locale.getLanguage()));
    }
}
