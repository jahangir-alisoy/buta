package az.buta.loyalty.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskProgressRepository extends JpaRepository<TaskProgress, UUID> {

    @Modifying
    @Query(value = "insert into task_progress (id, user_id, task_id, period_key, progress_value, target_value, status, awarded, award_seq, updated_at) "
            + "values (:id, :userId, :taskId, :periodKey, 0, :target, 'IN_PROGRESS', false, 0, :now) "
            + "on conflict (user_id, task_id, period_key) do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId, @Param("taskId") UUID taskId,
                       @Param("periodKey") String periodKey, @Param("target") int target, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TaskProgress p where p.userId = :userId and p.taskId = :taskId and p.periodKey = :periodKey")
    Optional<TaskProgress> findForUpdate(@Param("userId") UUID userId, @Param("taskId") UUID taskId, @Param("periodKey") String periodKey);

    List<TaskProgress> findByUserId(UUID userId);
}
