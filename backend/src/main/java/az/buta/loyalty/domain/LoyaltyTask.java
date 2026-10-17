package az.buta.loyalty.domain;

import az.buta.common.domain.AbstractEntity;
import az.buta.common.i18n.LocalizedText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loyalty_tasks")
public class LoyaltyTask extends AbstractEntity {

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText terms;

    @Column(name = "target_value", nullable = false)
    private int targetValue;

    @Column(name = "reward_points", nullable = false)
    private long rewardPoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private TaskPeriod periodType;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "min_order_amount", nullable = false)
    private BigDecimal minOrderAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected LoyaltyTask() {
    }

    public static LoyaltyTask create(String code, TaskType taskType, Instant now) {
        LoyaltyTask task = new LoyaltyTask();
        task.code = code;
        task.taskType = taskType;
        task.createdAt = now;
        return task;
    }

    public void update(LocalizedText title, LocalizedText description, LocalizedText terms, int targetValue, long rewardPoints,
                       TaskPeriod periodType, Instant startsAt, Instant endsAt, boolean active, BigDecimal minOrderAmount, Instant now) {
        this.title = title;
        this.description = description;
        this.terms = terms;
        this.targetValue = targetValue;
        this.rewardPoints = rewardPoints;
        this.periodType = periodType;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.active = active;
        this.minOrderAmount = minOrderAmount;
        this.updatedAt = now;
    }

    public boolean isRunningAt(Instant instant) {
        return active && !instant.isBefore(startsAt) && (endsAt == null || instant.isBefore(endsAt));
    }

    public String code() {
        return code;
    }

    public TaskType taskType() {
        return taskType;
    }

    public LocalizedText title() {
        return title;
    }

    public LocalizedText description() {
        return description;
    }

    public LocalizedText terms() {
        return terms;
    }

    public int targetValue() {
        return targetValue;
    }

    public long rewardPoints() {
        return rewardPoints;
    }

    public TaskPeriod periodType() {
        return periodType;
    }

    public Instant startsAt() {
        return startsAt;
    }

    public Instant endsAt() {
        return endsAt;
    }

    public boolean active() {
        return active;
    }

    public BigDecimal minOrderAmount() {
        return minOrderAmount;
    }
}
