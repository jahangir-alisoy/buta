package az.buta.recommendation.domain;

import az.buta.common.domain.NewAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_interest_profiles")
public class UserInterestProfile extends NewAwareEntity<UUID> {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_affinity", nullable = false)
    private Map<String, Double> categoryAffinity = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "partner_affinity", nullable = false)
    private Map<String, Double> partnerAffinity = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "purchased_category_ids", nullable = false)
    private List<String> purchasedCategoryIds = new ArrayList<>();

    @Column(name = "price_band_low")
    private BigDecimal priceBandLow;

    @Column(name = "price_band_high")
    private BigDecimal priceBandHigh;

    @Column(name = "price_sensitivity", nullable = false)
    private BigDecimal priceSensitivity = BigDecimal.ZERO;

    @Column(name = "signals_count", nullable = false)
    private int signalsCount;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    protected UserInterestProfile() {
    }

    public UserInterestProfile(UUID userId) {
        this.userId = userId;
    }

    @Override
    public UUID getId() {
        return userId;
    }

    public void replace(InterestProfile profile, Instant now) {
        this.categoryAffinity = toStringKeys(profile.categoryAffinity());
        this.partnerAffinity = toStringKeys(profile.partnerAffinity());
        this.purchasedCategoryIds = profile.purchasedCategoryIds().stream().map(UUID::toString).toList();
        this.priceBandLow = profile.priceLow();
        this.priceBandHigh = profile.priceHigh();
        this.priceSensitivity = BigDecimal.valueOf(profile.priceSensitivity()).setScale(2, java.math.RoundingMode.HALF_UP);
        this.signalsCount = profile.signals();
        this.computedAt = now;
    }

    public InterestProfile toProfile() {
        Map<UUID, Double> categories = new HashMap<>();
        categoryAffinity.forEach((key, value) -> categories.put(UUID.fromString(key), value));
        Map<UUID, Double> partners = new HashMap<>();
        partnerAffinity.forEach((key, value) -> partners.put(UUID.fromString(key), value));
        return new InterestProfile(categories, partners, purchasedCategoryIds.stream().map(UUID::fromString).collect(java.util.stream.Collectors.toSet()),
                priceBandLow, priceBandHigh, priceSensitivity.doubleValue(), signalsCount);
    }

    public Instant computedAt() {
        return computedAt;
    }

    private static Map<String, Double> toStringKeys(Map<UUID, Double> source) {
        Map<String, Double> target = new HashMap<>();
        source.forEach((key, value) -> target.put(key.toString(), Math.round(value * 100.0) / 100.0));
        return target;
    }
}
