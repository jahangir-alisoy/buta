package az.buta.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("select a from AuditLog a where (:action is null or a.action = :action) "
            + "and (:targetType is null or a.targetType = :targetType) order by a.createdAt desc")
    Page<AuditLog> search(@Param("action") String action, @Param("targetType") String targetType, Pageable pageable);

    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
}
