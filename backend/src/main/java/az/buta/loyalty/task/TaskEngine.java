package az.buta.loyalty.task;

import az.buta.common.config.AppProperties;
import az.buta.loyalty.LedgerService;
import az.buta.loyalty.domain.LedgerEntryType;
import az.buta.loyalty.domain.LedgerSourceType;
import az.buta.loyalty.domain.LoyaltyTask;
import az.buta.loyalty.domain.LoyaltyTaskRepository;
import az.buta.loyalty.domain.TaskProgress;
import az.buta.loyalty.domain.TaskProgressRepository;
import az.buta.loyalty.domain.TaskType;
import az.buta.membership.MembershipFeature;
import az.buta.membership.MembershipService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskEngine {

    private final LoyaltyTaskRepository tasks;
    private final TaskProgressRepository progress;
    private final UserFactsLoader factsLoader;
    private final LedgerService ledger;
    private final MembershipService membership;
    private final Map<TaskType, TaskEvaluator> evaluators = new EnumMap<>(TaskType.class);
    private final ZoneId zone;
    private final Clock clock;

    public TaskEngine(LoyaltyTaskRepository tasks, TaskProgressRepository progress, UserFactsLoader factsLoader, LedgerService ledger,
                      MembershipService membership, List<TaskEvaluator> evaluatorList, AppProperties properties, Clock clock) {
        this.tasks = tasks;
        this.progress = progress;
        this.factsLoader = factsLoader;
        this.ledger = ledger;
        this.membership = membership;
        evaluatorList.forEach(evaluator -> evaluators.put(evaluator.type(), evaluator));
        this.zone = properties.businessTimeZone();
        this.clock = clock;
    }

    @Transactional
    public void evaluate(UUID userId, Instant reference) {
        List<LoyaltyTask> running = tasks.findByActiveTrue().stream().filter(task -> task.isRunningAt(reference)).toList();
        if (running.isEmpty()) {
            return;
        }
        UserFacts facts = factsLoader.load(userId);
        boolean canEarn = membership.hasFeature(userId, MembershipFeature.LOYALTY_TASKS);
        Instant now = clock.instant();
        for (LoyaltyTask task : running) {
            TaskWindow window = TaskWindows.forTask(task, reference, zone);
            progress.insertIfAbsent(UUID.randomUUID(), userId, task.getId(), window.periodKey(), task.targetValue(), now);
            TaskProgress row = progress.findForUpdate(userId, task.getId(), window.periodKey()).orElseThrow();
            int value = evaluators.get(task.taskType()).progress(window, facts, task.minOrderAmount());
            row.track(value, task.targetValue(), now);
            boolean satisfied = value >= task.targetValue();
            if (satisfied && !row.awarded() && canEarn) {
                int sequence = row.nextAwardSeq();
                ledger.post(new LedgerService.PostingRequest(userId, LedgerEntryType.EARN_TASK, task.rewardPoints(),
                        awardKey(task.getId(), userId, window.periodKey(), sequence), LedgerSourceType.TASK, task.getId(), null,
                        "ledger.earnTask", Map.of("taskCode", task.code()), null));
            } else if (!satisfied && row.awarded()) {
                ledger.reverse(awardKey(task.getId(), userId, window.periodKey(), row.awardSeq()), "ledger.taskReversal", null);
                row.markAwardReversed();
            }
        }
    }

    @Transactional
    public void evaluateCurrent(UUID userId) {
        evaluate(userId, clock.instant());
    }

    public static String awardKey(UUID taskId, UUID userId, String periodKey, int sequence) {
        return "EARN_TASK:" + taskId + ":" + userId + ":" + periodKey + ":" + sequence;
    }
}
