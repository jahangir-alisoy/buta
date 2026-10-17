package az.buta.membership;

import az.buta.common.domain.NewAwareEntity;
import az.buta.common.i18n.LocalizedText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan extends NewAwareEntity<PlanCode> {

    @Id
    @Enumerated(EnumType.STRING)
    private PlanCode code;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> features;

    private BigDecimal price;

    @Column(name = "billing_period")
    private String billingPeriod;

    @Column(nullable = false)
    private boolean purchasable;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MembershipPlan() {
    }

    @Override
    public PlanCode getId() {
        return code;
    }

    public Set<MembershipFeature> featureSet() {
        return features.stream().map(MembershipFeature::valueOf).collect(Collectors.toUnmodifiableSet());
    }

    public void changeFeatures(Set<MembershipFeature> newFeatures, Instant now) {
        this.features = newFeatures.stream().map(Enum::name).sorted().toList();
        this.updatedAt = now;
    }

    public PlanCode code() {
        return code;
    }

    public LocalizedText name() {
        return name;
    }

    public LocalizedText description() {
        return description;
    }

    public boolean purchasable() {
        return purchasable;
    }
}
