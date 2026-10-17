package az.buta.catalog.domain;

import az.buta.common.domain.AbstractEntity;
import az.buta.common.i18n.LocalizedText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "campaigns")
public class Campaign extends AbstractEntity {

    @Column(nullable = false)
    private String slug;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "banner_text", nullable = false)
    private LocalizedText bannerText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignAudience audience;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "bonus_points_per_unit", nullable = false)
    private int bonusPointsPerUnit;

    @Column(nullable = false)
    private boolean featured;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActiveStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany
    @JoinTable(name = "campaign_offers", joinColumns = @JoinColumn(name = "campaign_id"),
            inverseJoinColumns = @JoinColumn(name = "offer_id"))
    private Set<Offer> offers = new HashSet<>();

    protected Campaign() {
    }

    public static Campaign create(String slug, Instant now) {
        Campaign campaign = new Campaign();
        campaign.slug = slug;
        campaign.createdAt = now;
        campaign.status = ActiveStatus.ACTIVE;
        return campaign;
    }

    public void update(LocalizedText name, LocalizedText bannerText, CampaignAudience audience, Instant startsAt, Instant endsAt,
                       int bonusPointsPerUnit, boolean featured, ActiveStatus status, Set<Offer> offers, Instant now) {
        this.name = name;
        this.bannerText = bannerText;
        this.audience = audience;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.bonusPointsPerUnit = bonusPointsPerUnit;
        this.featured = featured;
        this.status = status;
        this.offers = new HashSet<>(offers);
        this.updatedAt = now;
    }

    public void shift(Duration shift, Instant now) {
        this.startsAt = startsAt.plus(shift);
        this.endsAt = endsAt.plus(shift);
        this.updatedAt = now;
    }

    public boolean isRunningAt(Instant now) {
        return status == ActiveStatus.ACTIVE && !now.isBefore(startsAt) && now.isBefore(endsAt);
    }

    public String slug() {
        return slug;
    }

    public LocalizedText name() {
        return name;
    }

    public LocalizedText bannerText() {
        return bannerText;
    }

    public CampaignAudience audience() {
        return audience;
    }

    public Instant startsAt() {
        return startsAt;
    }

    public Instant endsAt() {
        return endsAt;
    }

    public int bonusPointsPerUnit() {
        return bonusPointsPerUnit;
    }

    public boolean featured() {
        return featured;
    }

    public ActiveStatus status() {
        return status;
    }

    public Set<Offer> offers() {
        return Set.copyOf(offers);
    }
}
