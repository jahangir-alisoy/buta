package az.buta.pricing;

import az.buta.common.domain.NewAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "business_settings")
public class BusinessSetting extends NewAwareEntity<String> {

    @Id
    private String key;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> value;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected BusinessSetting() {
    }

    @Override
    public String getId() {
        return key;
    }

    public Map<String, Object> value() {
        return value;
    }

    public void replace(Map<String, Object> newValue, UUID updatedBy, Instant now) {
        this.value = newValue;
        this.updatedBy = updatedBy;
        this.updatedAt = now;
    }
}
