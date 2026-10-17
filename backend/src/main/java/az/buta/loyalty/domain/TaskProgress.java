package az.buta.loyalty.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_progress")
public class TaskProgress extends AbstractEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "period_key", nullable = false)
    private String periodKey;

    @Column(name = "progress_value", nullable = false)
    private int progressValue;

    @Column(name = "target_value", nullable = false)
    private int targetValue;

    @Column(nullable = false)
    private String status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false)
    private boolean awarded;

    @Column(name = "award_seq", nullable = false)
    private int awardSeq;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected TaskProgress() {
    }

    public void track(int progress, int target, Instant now) {
        this.progressValue = progress;
        this.targetValue = target;
        this.updatedAt = now;
        if (progress >= target) {
            if (!"COMPLETED".equals(status)) {
                completedAt = now;
            }
            status = "COMPLETED";
        } else {
            status = "IN_PROGRESS";
            completedAt = null;
        }
    }

    public int nextAwardSeq() {
        awardSeq++;
        awarded = true;
        return awardSeq;
    }

    public void markAwardReversed() {
        awarded = false;
    }

    public UUID userId() {
        return userId;
    }

    public UUID taskId() {
        return taskId;
    }

    public String periodKey() {
        return periodKey;
    }

    public int progressValue() {
        return progressValue;
    }

    public int targetValue() {
        return targetValue;
    }

    public String status() {
        return status;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public boolean awarded() {
        return awarded;
    }

    public int awardSeq() {
        return awardSeq;
    }
}
