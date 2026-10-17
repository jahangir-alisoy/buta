package az.buta.user.domain;

import az.buta.common.domain.NewAwareEntity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferences extends NewAwareEntity<UUID> {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Theme theme;

    @Column(name = "personalization_enabled", nullable = false)
    private boolean personalizationEnabled;

    @Column(name = "personalization_decided_at")
    private Instant personalizationDecidedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_category_ids", nullable = false)
    private List<UUID> preferredCategoryIds = new ArrayList<>();

    @Column(name = "budget_min")
    private BigDecimal budgetMin;

    @Column(name = "budget_max")
    private BigDecimal budgetMax;

    private String city;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPreferences() {
    }

    public static UserPreferences defaults(UUID userId, String locale, Instant now) {
        UserPreferences preferences = new UserPreferences();
        preferences.userId = userId;
        preferences.locale = "en".equals(locale) ? "en" : "az";
        preferences.theme = Theme.SYSTEM;
        preferences.updatedAt = now;
        return preferences;
    }

    @Override
    public UUID getId() {
        return userId;
    }

    public void update(String locale, Theme theme, List<UUID> preferredCategoryIds, BigDecimal budgetMin, BigDecimal budgetMax,
                       String city, Instant now) {
        this.locale = locale;
        this.theme = theme;
        this.preferredCategoryIds = new ArrayList<>(preferredCategoryIds);
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.city = city;
        this.updatedAt = now;
    }

    public void decidePersonalization(boolean enabled, Instant now) {
        this.personalizationEnabled = enabled;
        this.personalizationDecidedAt = now;
        this.updatedAt = now;
    }

    public String locale() {
        return locale;
    }

    public Theme theme() {
        return theme;
    }

    public boolean personalizationEnabled() {
        return personalizationEnabled;
    }

    public Instant personalizationDecidedAt() {
        return personalizationDecidedAt;
    }

    public List<UUID> preferredCategoryIds() {
        return List.copyOf(preferredCategoryIds);
    }

    public BigDecimal budgetMin() {
        return budgetMin;
    }

    public BigDecimal budgetMax() {
        return budgetMax;
    }

    public String city() {
        return city;
    }
}
