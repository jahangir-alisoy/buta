package az.buta.catalog.domain;

import az.buta.common.domain.AbstractEntity;
import az.buta.common.i18n.LocalizedText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "categories")
public class Category extends AbstractEntity {

    @Column(nullable = false)
    private String slug;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText name;

    @Column(name = "icon_key", nullable = false)
    private String iconKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Category() {
    }

    public static Category create(String slug, LocalizedText name, String iconKey, int sortOrder, Instant now) {
        Category category = new Category();
        category.slug = slug;
        category.createdAt = now;
        category.update(name, iconKey, sortOrder, true, now);
        return category;
    }

    public void update(LocalizedText name, String iconKey, int sortOrder, boolean active, Instant now) {
        this.name = name;
        this.iconKey = iconKey;
        this.sortOrder = sortOrder;
        this.active = active;
        this.updatedAt = now;
    }

    public String slug() {
        return slug;
    }

    public LocalizedText name() {
        return name;
    }

    public String iconKey() {
        return iconKey;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public boolean active() {
        return active;
    }
}
