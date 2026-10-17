package az.buta.catalog.domain;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "offer_components")
public class OfferComponent extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id")
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private PartnerProduct product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "negotiated_unit_cost")
    private BigDecimal negotiatedUnitCost;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected OfferComponent() {
    }

    OfferComponent(Offer offer, PartnerProduct product, int quantity, BigDecimal negotiatedUnitCost, int sortOrder) {
        this.offer = offer;
        this.product = product;
        this.quantity = quantity;
        this.negotiatedUnitCost = negotiatedUnitCost;
        this.sortOrder = sortOrder;
    }

    public PartnerProduct product() {
        return product;
    }

    public int quantity() {
        return quantity;
    }

    public BigDecimal negotiatedUnitCost() {
        return negotiatedUnitCost;
    }

    public int sortOrder() {
        return sortOrder;
    }
}
