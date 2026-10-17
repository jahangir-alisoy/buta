package az.buta.loyalty.domain;

import az.buta.common.domain.NewAwareEntity;
import az.buta.common.i18n.LocalizedText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "loyalty_tiers")
public class LoyaltyTier extends NewAwareEntity<String> {

    @Id
    private String code;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText name;

    @Column(name = "threshold_points", nullable = false)
    private long thresholdPoints;

    @Column(name = "earn_multiplier", nullable = false)
    private BigDecimal earnMultiplier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, List<String>> benefits;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected LoyaltyTier() {
    }

    @Override
    public String getId() {
        return code;
    }

    public void update(LocalizedText name, long thresholdPoints, BigDecimal earnMultiplier, Map<String, List<String>> benefits) {
        this.name = name;
        this.thresholdPoints = thresholdPoints;
        this.earnMultiplier = earnMultiplier;
        this.benefits = benefits;
    }

    public String code() {
        return code;
    }

    public LocalizedText name() {
        return name;
    }

    public long thresholdPoints() {
        return thresholdPoints;
    }

    public BigDecimal earnMultiplier() {
        return earnMultiplier;
    }

    public List<String> benefits(String language) {
        return benefits.getOrDefault("en".equals(language) ? "en" : "az", List.of());
    }

    public Map<String, List<String>> benefits() {
        return benefits;
    }

    public int sortOrder() {
        return sortOrder;
    }
}
