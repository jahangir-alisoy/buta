package az.buta.pricing;

import az.buta.audit.AuditService;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class BusinessSettingsService {

    private final BusinessSettingRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditService audit;
    private final Clock clock;
    private final Cache<SettingsKey, Object> cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build();

    public BusinessSettingsService(BusinessSettingRepository repository, ObjectMapper objectMapper, AuditService audit, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.audit = audit;
        this.clock = clock;
    }

    public PricingSettings pricing() {
        return get(SettingsKey.PRICING, PricingSettings.class);
    }

    public LoyaltySettings loyalty() {
        return get(SettingsKey.LOYALTY, LoyaltySettings.class);
    }

    public MembershipSettings membership() {
        return get(SettingsKey.MEMBERSHIP, MembershipSettings.class);
    }

    public RecommendationSettings recommendation() {
        return get(SettingsKey.RECOMMENDATION, RecommendationSettings.class);
    }

    public Object raw(SettingsKey key) {
        return get(key, key.type());
    }

    @Transactional
    public void update(SettingsKey key, Object typedValue, UUID adminId) {
        if (typedValue instanceof RecommendationSettings settings) {
            validateWeights(settings);
        }
        Map<String, Object> value = objectMapper.convertValue(typedValue, new TypeReference<>() {
        });
        BusinessSetting setting = repository.findById(key.key()).orElseThrow(ApiException::notFound);
        Map<String, Object> previous = setting.value();
        setting.replace(value, adminId, clock.instant());
        cache.invalidate(key);
        audit.admin(adminId, "SETTINGS_UPDATED", "SETTINGS", key.key(), Map.of("before", previous, "after", value));
    }

    public void evictAll() {
        cache.invalidateAll();
    }

    private static void validateWeights(RecommendationSettings settings) {
        if (settings.weights().total() != 100) {
            throw ApiException.field(ErrorCode.WEIGHTS_INVALID, "weights");
        }
        settings.variants().stream()
                .filter(variant -> variant.weights() != null && variant.weights().total() != 100)
                .findAny()
                .ifPresent(variant -> {
                    throw ApiException.field(ErrorCode.WEIGHTS_INVALID, "variants");
                });
        int allocation = settings.variants().stream().mapToInt(RecommendationSettings.Variant::allocationPercent).sum();
        if (allocation != 100) {
            throw ApiException.field(ErrorCode.WEIGHTS_INVALID, "variants");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T get(SettingsKey key, Class<T> type) {
        return (T) cache.get(key, ignored -> repository.findById(key.key())
                .map(setting -> objectMapper.convertValue(setting.value(), type))
                .orElseThrow(() -> new IllegalStateException("Missing business setting " + key.key())));
    }
}
