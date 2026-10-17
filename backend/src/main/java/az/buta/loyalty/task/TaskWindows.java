package az.buta.loyalty.task;

import az.buta.loyalty.domain.LoyaltyTask;
import az.buta.loyalty.domain.TaskPeriod;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public final class TaskWindows {

    private static final Instant FAR_FUTURE = Instant.parse("9999-01-01T00:00:00Z");

    private TaskWindows() {
    }

    public static TaskWindow forTask(LoyaltyTask task, Instant reference, ZoneId zone) {
        Instant taskEnd = task.endsAt() == null ? FAR_FUTURE : task.endsAt();
        if (task.periodType() == TaskPeriod.ONE_TIME) {
            return new TaskWindow("ONE_TIME", task.startsAt(), taskEnd);
        }
        YearMonth month = YearMonth.from(reference.atZone(zone));
        Instant monthStart = month.atDay(1).atStartOfDay(zone).toInstant();
        Instant monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        Instant start = monthStart.isBefore(task.startsAt()) ? task.startsAt() : monthStart;
        Instant end = monthEnd.isAfter(taskEnd) ? taskEnd : monthEnd;
        return new TaskWindow(month.toString(), start.truncatedTo(ChronoUnit.SECONDS), end);
    }
}
