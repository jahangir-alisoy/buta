package az.buta.loyalty.task;

import az.buta.loyalty.domain.LoyaltyTask;
import az.buta.loyalty.domain.TaskPeriod;
import az.buta.loyalty.domain.TaskType;
import az.buta.common.i18n.LocalizedText;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TaskEvaluatorsTest {

    private static final BigDecimal MIN = new BigDecimal("5.00");
    private static final TaskWindow OCTOBER = new TaskWindow("2026-10", Instant.parse("2026-09-30T20:00:00Z"), Instant.parse("2026-10-31T20:00:00Z"));
    private static final UUID CINEMA = UUID.randomUUID();
    private static final UUID THEATRE = UUID.randomUUID();

    @Test
    void distinctPartnersCountsUniquePartnersInWindowAboveMinimum() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UserFacts facts = facts(
                purchase(false, CINEMA, Set.of(first), "2026-10-02T10:00:00Z", "10.00"),
                purchase(false, CINEMA, Set.of(first), "2026-10-03T10:00:00Z", "10.00"),
                purchase(true, THEATRE, Set.of(first, second), "2026-10-05T10:00:00Z", "30.00"),
                purchase(false, THEATRE, Set.of(UUID.randomUUID()), "2026-10-06T10:00:00Z", "3.00"),
                purchase(false, THEATRE, Set.of(UUID.randomUUID()), "2026-09-01T10:00:00Z", "30.00"));
        assertThat(new DistinctPartnersEvaluator().progress(OCTOBER, facts, MIN)).isEqualTo(2);
    }

    @Test
    void bundleEvaluatorCountsOnlyBundles() {
        UserFacts facts = facts(purchase(true, CINEMA, Set.of(UUID.randomUUID()), "2026-10-02T10:00:00Z", "28.00"),
                purchase(false, CINEMA, Set.of(UUID.randomUUID()), "2026-10-02T11:00:00Z", "28.00"));
        assertThat(new BundlePurchaseEvaluator().progress(OCTOBER, facts, MIN)).isEqualTo(1);
    }

    @Test
    void newCategoryRequiresEarlierHistoryAndANewCategory() {
        UserFacts onlyNew = facts(purchase(false, THEATRE, Set.of(UUID.randomUUID()), "2026-10-02T10:00:00Z", "20.00"));
        assertThat(new NewCategoryEvaluator().progress(OCTOBER, onlyNew, MIN)).isZero();
        UserFacts withHistory = facts(purchase(false, CINEMA, Set.of(UUID.randomUUID()), "2026-08-02T10:00:00Z", "20.00"),
                purchase(false, CINEMA, Set.of(UUID.randomUUID()), "2026-10-02T10:00:00Z", "20.00"),
                purchase(false, THEATRE, Set.of(UUID.randomUUID()), "2026-10-03T10:00:00Z", "20.00"));
        assertThat(new NewCategoryEvaluator().progress(OCTOBER, withHistory, MIN)).isEqualTo(1);
    }

    @Test
    void attendanceAndReferralsCountEventsInsideWindow() {
        UserFacts facts = new UserFacts(List.of(), List.of(Instant.parse("2026-10-04T18:00:00Z"), Instant.parse("2026-11-04T18:00:00Z")),
                List.of(Instant.parse("2026-10-10T18:00:00Z")));
        assertThat(new EventAttendanceEvaluator().progress(OCTOBER, facts, MIN)).isEqualTo(1);
        assertThat(new ReferralEvaluator().progress(OCTOBER, facts, MIN)).isEqualTo(1);
    }

    @Test
    void calendarMonthWindowsFollowBakuTime() {
        LoyaltyTask task = LoyaltyTask.create("monthly", TaskType.DISTINCT_PARTNERS, Instant.parse("2026-01-01T00:00:00Z"));
        task.update(LocalizedText.of("a", "a"), LocalizedText.of("a", "a"), LocalizedText.of("a", "a"), 3, 60, TaskPeriod.CALENDAR_MONTH,
                Instant.parse("2026-01-01T00:00:00Z"), null, true, MIN, Instant.parse("2026-01-01T00:00:00Z"));
        TaskWindow window = TaskWindows.forTask(task, Instant.parse("2026-10-31T21:30:00Z"), ZoneId.of("Asia/Baku"));
        assertThat(window.periodKey()).isEqualTo("2026-11");
        assertThat(window.start()).isEqualTo(Instant.parse("2026-10-31T20:00:00Z"));
    }

    private static UserFacts facts(PurchaseFact... purchases) {
        return new UserFacts(List.of(purchases), List.of(), List.of());
    }

    private static PurchaseFact purchase(boolean bundle, UUID category, Set<UUID> partners, String at, String total) {
        return new PurchaseFact(UUID.randomUUID(), UUID.randomUUID(), bundle, category, partners, Instant.parse(at), new BigDecimal(total));
    }
}
