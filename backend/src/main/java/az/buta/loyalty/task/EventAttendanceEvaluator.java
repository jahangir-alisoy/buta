package az.buta.loyalty.task;

import az.buta.loyalty.domain.TaskType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EventAttendanceEvaluator implements TaskEvaluator {

    @Override
    public TaskType type() {
        return TaskType.EVENT_ATTENDANCE;
    }

    @Override
    public int progress(TaskWindow window, UserFacts facts, BigDecimal minOrderAmount) {
        return (int) facts.eventAttendances().stream().filter(window::contains).count();
    }
}
