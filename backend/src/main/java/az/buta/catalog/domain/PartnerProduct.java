package az.buta.catalog.domain;

import az.buta.common.domain.AbstractEntity;
import az.buta.common.i18n.LocalizedText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "partner_products")
public class PartnerProduct extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id")
    private Partner partner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private LocalizedText terms;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "redemption_instructions", nullable = false)
    private LocalizedText redemptionInstructions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligibility_note")
    private LocalizedText eligibilityNote;

    @Column(name = "retail_price", nullable = false)
    private BigDecimal retailPrice;

    @Column(nullable = false)
    private String currency = "AZN";

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_model", nullable = false)
    private CostModel costModel;

    @Column(name = "unit_net_cost")
    private BigDecimal unitNetCost;

    @Column(name = "commission_percent")
    private BigDecimal commissionPercent;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "event_starts_at")
    private Instant eventStartsAt;

    @Column(name = "event_ends_at")
    private Instant eventEndsAt;

    @Column(name = "venue_name")
    private String venueName;

    @Column(name = "venue_address")
    private String venueAddress;

    @Column(nullable = false)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "redemption_method", nullable = false)
    private RedemptionMethod redemptionMethod;

    @Column(nullable = false)
    private boolean refundable;

    @Column(name = "refund_deadline_hours", nullable = false)
    private int refundDeadlineHours;

    @Column(name = "restock_on_refund", nullable = false)
    private boolean restockOnRefund;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActiveStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected PartnerProduct() {
    }

    public static PartnerProduct create(Partner partner, Instant now) {
        PartnerProduct product = new PartnerProduct();
        product.partner = partner;
        product.createdAt = now;
        product.status = ActiveStatus.ACTIVE;
        return product;
    }

    public void describe(Category category, ProductType productType, LocalizedText title, LocalizedText description, LocalizedText terms,
                         LocalizedText redemptionInstructions, LocalizedText eligibilityNote, RedemptionMethod redemptionMethod, Instant now) {
        this.category = category;
        this.productType = productType;
        this.title = title;
        this.description = description;
        this.terms = terms;
        this.redemptionInstructions = redemptionInstructions;
        this.eligibilityNote = eligibilityNote;
        this.redemptionMethod = redemptionMethod;
        this.updatedAt = now;
    }

    public void price(BigDecimal retailPrice, CostModel costModel, BigDecimal unitNetCost, BigDecimal commissionPercent, Instant now) {
        this.retailPrice = retailPrice;
        this.costModel = costModel;
        this.unitNetCost = costModel == CostModel.WHOLESALE ? unitNetCost : null;
        this.commissionPercent = costModel == CostModel.COMMISSION ? commissionPercent : null;
        this.updatedAt = now;
    }

    public void schedule(Instant validFrom, Instant validUntil, Instant eventStartsAt, Instant eventEndsAt, String venueName,
                         String venueAddress, String city, Instant now) {
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.eventStartsAt = eventStartsAt;
        this.eventEndsAt = eventEndsAt;
        this.venueName = venueName;
        this.venueAddress = venueAddress;
        this.city = city;
        this.updatedAt = now;
    }

    public void refundPolicy(boolean refundable, int refundDeadlineHours, boolean restockOnRefund, ActiveStatus status, Instant now) {
        this.refundable = refundable;
        this.refundDeadlineHours = refundDeadlineHours;
        this.restockOnRefund = restockOnRefund;
        this.status = status;
        this.updatedAt = now;
    }

    public void shiftDates(java.time.Duration shift, Instant now) {
        this.validFrom = validFrom.plus(shift);
        this.validUntil = validUntil.plus(shift);
        if (eventStartsAt != null) {
            this.eventStartsAt = eventStartsAt.plus(shift);
        }
        if (eventEndsAt != null) {
            this.eventEndsAt = eventEndsAt.plus(shift);
        }
        this.updatedAt = now;
    }

    public boolean isUsableAt(Instant now) {
        return status == ActiveStatus.ACTIVE && partner.isActive() && now.isBefore(validUntil)
                && (eventStartsAt == null || now.isBefore(eventStartsAt));
    }

    public Instant refundDeadline() {
        Instant reference = eventStartsAt != null ? eventStartsAt : validUntil;
        return reference.minusSeconds(refundDeadlineHours * 3600L);
    }

    public Partner partner() {
        return partner;
    }

    public Category category() {
        return category;
    }

    public ProductType productType() {
        return productType;
    }

    public LocalizedText title() {
        return title;
    }

    public LocalizedText description() {
        return description;
    }

    public LocalizedText terms() {
        return terms;
    }

    public LocalizedText redemptionInstructions() {
        return redemptionInstructions;
    }

    public LocalizedText eligibilityNote() {
        return eligibilityNote;
    }

    public BigDecimal retailPrice() {
        return retailPrice;
    }

    public CostModel costModel() {
        return costModel;
    }

    public BigDecimal unitNetCost() {
        return unitNetCost;
    }

    public BigDecimal commissionPercent() {
        return commissionPercent;
    }

    public Instant validFrom() {
        return validFrom;
    }

    public Instant validUntil() {
        return validUntil;
    }

    public Instant eventStartsAt() {
        return eventStartsAt;
    }

    public Instant eventEndsAt() {
        return eventEndsAt;
    }

    public String venueName() {
        return venueName;
    }

    public String venueAddress() {
        return venueAddress;
    }

    public String city() {
        return city;
    }

    public RedemptionMethod redemptionMethod() {
        return redemptionMethod;
    }

    public boolean refundable() {
        return refundable;
    }

    public int refundDeadlineHours() {
        return refundDeadlineHours;
    }

    public boolean restockOnRefund() {
        return restockOnRefund;
    }

    public ActiveStatus status() {
        return status;
    }
}
