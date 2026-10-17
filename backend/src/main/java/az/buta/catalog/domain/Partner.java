package az.buta.catalog.domain;

import az.buta.common.domain.AbstractEntity;
import az.buta.common.i18n.LocalizedText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "partners")
public class Partner extends AbstractEntity {

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText description;

    @Column(nullable = false)
    private String city;

    private String address;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(nullable = false)
    private boolean demo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActiveStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Partner() {
    }

    public static Partner create(String slug, Instant now) {
        Partner partner = new Partner();
        partner.slug = slug;
        partner.createdAt = now;
        partner.status = ActiveStatus.ACTIVE;
        return partner;
    }

    public void update(String name, LocalizedText description, String city, String address, String websiteUrl, String logoPath,
                       boolean demo, ActiveStatus status, Instant now) {
        this.name = name;
        this.description = description;
        this.city = city;
        this.address = address;
        this.websiteUrl = websiteUrl;
        this.logoPath = logoPath;
        this.demo = demo;
        this.status = status;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return status == ActiveStatus.ACTIVE;
    }

    public String slug() {
        return slug;
    }

    public String name() {
        return name;
    }

    public LocalizedText description() {
        return description;
    }

    public String city() {
        return city;
    }

    public String address() {
        return address;
    }

    public String websiteUrl() {
        return websiteUrl;
    }

    public String logoPath() {
        return logoPath;
    }

    public boolean demo() {
        return demo;
    }

    public ActiveStatus status() {
        return status;
    }
}
