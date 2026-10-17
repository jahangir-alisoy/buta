package az.buta.audit;

import az.buta.common.crypto.IpHasher;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final IpHasher ipHasher;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, IpHasher ipHasher, Clock clock) {
        this.repository = repository;
        this.ipHasher = ipHasher;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(UUID actorUserId, ActorType actorType, String action, String targetType, Object targetId, Map<String, ?> details) {
        Map<String, Object> safeDetails = new LinkedHashMap<>();
        if (details != null) {
            details.forEach((key, value) -> safeDetails.put(key, value == null ? null : String.valueOf(value)));
        }
        repository.save(new AuditLog(actorUserId, actorType, action, targetType, targetId == null ? null : targetId.toString(),
                safeDetails, MDC.get("requestId"), ipHasher.hash(currentIp()), clock.instant()));
    }

    public void admin(UUID adminId, String action, String targetType, Object targetId, Map<String, ?> details) {
        record(adminId, ActorType.ADMIN, action, targetType, targetId, details);
    }

    public void user(UUID userId, String action, Map<String, ?> details) {
        record(userId, ActorType.USER, action, "USER", userId, details);
    }

    public void system(String action, String targetType, Object targetId, Map<String, ?> details) {
        record(null, ActorType.SYSTEM, action, targetType, targetId, details);
    }

    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
