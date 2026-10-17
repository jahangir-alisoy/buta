package az.buta.catalog.domain;

import az.buta.common.domain.AbstractEntity;
import az.buta.common.i18n.LocalizedText;
import az.buta.common.text.SearchKeys;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "offers")
public class Offer extends AbstractEntity {

    @Column(nullable = false)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", nullable = false)
    private OfferType offerType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_category_id")
    private Category primaryCategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText subtitle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText description;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "selling_price", nullable = false)
    private BigDecimal sellingPrice;

    @Column(nullable = false)
    private String currency = "AZN";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferStatus status;

    @Column(name = "sale_starts_at", nullable = false)
    private Instant saleStartsAt;

    @Column(name = "sale_ends_at", nullable = false)
    private Instant saleEndsAt;

    @Column(name = "max_per_order", nullable = false)
    private int maxPerOrder;

    @Column(name = "business_priority", nullable = false)
    private int businessPriority;

    @Column(nullable = false)
    private boolean featured;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> tags = new ArrayList<>();

    @Column(name = "search_key", nullable = false)
    private String searchKey = "";

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<OfferComponent> components = new ArrayList<>();

    protected Offer() {
    }

    public static Offer draft(String slug, Instant now) {
        Offer offer = new Offer();
        offer.slug = slug;
        offer.status = OfferStatus.DRAFT;
        offer.createdAt = now;
        offer.updatedAt = now;
        return offer;
    }

    public void describe(OfferType offerType, Category primaryCategory, LocalizedText title, LocalizedText subtitle,
                         LocalizedText description, String imagePath, List<String> tags, Instant now) {
        this.offerType = offerType;
        this.primaryCategory = primaryCategory;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.imagePath = imagePath;
        this.tags = new ArrayList<>(tags);
        this.updatedAt = now;
    }

    public void configureSale(BigDecimal sellingPrice, OfferVisibility visibility, Instant saleStartsAt, Instant saleEndsAt,
                              int maxPerOrder, int businessPriority, boolean featured, Instant now) {
        this.sellingPrice = sellingPrice;
        this.visibility = visibility;
        this.saleStartsAt = saleStartsAt;
        this.saleEndsAt = saleEndsAt;
        this.maxPerOrder = maxPerOrder;
        this.businessPriority = businessPriority;
        this.featured = featured;
        this.updatedAt = now;
    }

    public void replaceComponents(List<ComponentSpec> specs, Instant now) {
        components.clear();
        int order = 0;
        for (ComponentSpec spec : specs) {
            components.add(new OfferComponent(this, spec.product(), spec.quantity(), spec.negotiatedUnitCost(), order++));
        }
        this.updatedAt = now;
    }

    public void refreshSearchKey() {
        List<String> parts = new ArrayList<>();
        parts.add(title.az());
        parts.add(title.en());
        parts.add(subtitle.az());
        parts.add(subtitle.en());
        parts.add(primaryCategory.name().az());
        parts.add(primaryCategory.name().en());
        components.forEach(component -> {
            parts.add(component.product().partner().name());
            parts.add(component.product().title().az());
            parts.add(component.product().title().en());
        });
        parts.addAll(tags);
        String key = SearchKeys.build(parts.toArray(String[]::new));
        this.searchKey = key.length() > 1000 ? key.substring(0, 1000) : key;
    }

    public void publish(Instant now) {
        this.status = OfferStatus.PUBLISHED;
        this.publishedAt = publishedAt == null ? now : publishedAt;
        this.updatedAt = now;
    }

    public void changeStatus(OfferStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    public void shiftSaleWindow(Duration shift, Instant now) {
        this.saleStartsAt = saleStartsAt.plus(shift);
        this.saleEndsAt = saleEndsAt.plus(shift);
        this.updatedAt = now;
    }

    public boolean isOnSaleAt(Instant now) {
        return status == OfferStatus.PUBLISHED && !now.isBefore(saleStartsAt) && now.isBefore(saleEndsAt);
    }

    public List<UUID> categoryIds() {
        List<UUID> ids = new ArrayList<>();
        ids.add(primaryCategory.getId());
        components.forEach(component -> {
            if (!ids.contains(component.product().category().getId())) {
                ids.add(component.product().category().getId());
            }
        });
        return ids;
    }

    public Partner leadPartner() {
        return components.isEmpty() ? null : components.getFirst().product().partner();
    }

    public String slug() {
        return slug;
    }

    public OfferType offerType() {
        return offerType;
    }

    public Category primaryCategory() {
        return primaryCategory;
    }

    public LocalizedText title() {
        return title;
    }

    public LocalizedText subtitle() {
        return subtitle;
    }

    public LocalizedText description() {
        return description;
    }

    public String imagePath() {
        return imagePath;
    }

    public BigDecimal sellingPrice() {
        return sellingPrice;
    }

    public OfferVisibility visibility() {
        return visibility;
    }

    public OfferStatus status() {
        return status;
    }

    public Instant saleStartsAt() {
        return saleStartsAt;
    }

    public Instant saleEndsAt() {
        return saleEndsAt;
    }

    public int maxPerOrder() {
        return maxPerOrder;
    }

    public int businessPriority() {
        return businessPriority;
    }

    public boolean featured() {
        return featured;
    }

    public List<String> tags() {
        return List.copyOf(tags);
    }

    public String searchKeyValue() {
        return searchKey;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<OfferComponent> components() {
        return List.copyOf(components);
    }

    public record ComponentSpec(PartnerProduct product, int quantity, BigDecimal negotiatedUnitCost) {
    }
}
