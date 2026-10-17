package az.buta.loyalty.task;

import az.buta.loyalty.domain.TaskType;

import java.math.BigDecimal;

public interface TaskEvaluator {

    TaskType type();

    int progress(TaskWindow window, UserFacts facts, BigDecimal minOrderAmount);
}
