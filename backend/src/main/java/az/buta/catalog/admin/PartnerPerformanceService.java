package az.buta.catalog.admin;

import az.buta.catalog.InventoryService;
import az.buta.catalog.domain.Partner;
import az.buta.catalog.domain.PartnerProduct;
import az.buta.catalog.domain.PartnerProductRepository;
import az.buta.catalog.domain.PartnerRepository;
import az.buta.catalog.domain.ProductInventory;
import az.buta.common.error.ApiException;
import az.buta.common.money.Amounts;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PartnerPerformanceService {

    private final PartnerRepository partners;
    private final PartnerProductRepository products;
    private final InventoryService inventory;
    private final EntityManager entityManager;

    public PartnerPerformanceService(PartnerRepository partners, PartnerProductRepository products, InventoryService inventory,
                                     EntityManager entityManager) {
        this.partners = partners;
        this.products = products;
        this.inventory = inventory;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public AdminCatalogDtos.PartnerPerformance performance(UUID partnerId, Instant from, Instant to) {
        Partner partner = partners.findById(partnerId).orElseThrow(ApiException::notFound);
        Object[] sales = (Object[]) entityManager.createNativeQuery(
                        "select coalesce(sum(case when i.refunded_quantity = 0 then c.quantity else 0 end), 0), "
                                + "coalesce(sum(case when i.refunded_quantity = 0 then c.partner_cost else 0 end), 0), "
                                + "coalesce(sum(case when i.refunded_quantity > 0 then c.quantity else 0 end), 0) "
                                + "from order_item_components c join order_items i on i.id = c.order_item_id join orders o on o.id = i.order_id "
                                + "where c.partner_id = :partnerId and o.fulfilled_at >= :from and o.fulfilled_at < :to")
                .setParameter("partnerId", partnerId).setParameter("from", from).setParameter("to", to)
                .getSingleResult();
        Object[] vouchers = (Object[]) entityManager.createNativeQuery(
                        "select count(*), coalesce(sum(case when r.status = 'REDEEMED' then 1 else 0 end), 0) from redemptions r "
                                + "join partner_products p on p.id = r.product_id where p.partner_id = :partnerId and r.status <> 'VOIDED' "
                                + "and r.created_at >= :from and r.created_at < :to")
                .setParameter("partnerId", partnerId).setParameter("from", from).setParameter("to", to)
                .getSingleResult();
        long issued = ((Number) vouchers[0]).longValue();
        long redeemed = ((Number) vouchers[1]).longValue();
        List<PartnerProduct> partnerProducts = products.findByPartnerId(partnerId);
        Map<UUID, ProductInventory> stock = inventory.snapshot(partnerProducts.stream().map(PartnerProduct::getId).toList());
        List<AdminCatalogDtos.ProductPerformance> productRows = partnerProducts.stream()
                .map(product -> {
                    ProductInventory row = stock.get(product.getId());
                    int total = row == null ? 0 : row.totalQuantity();
                    int sold = row == null ? 0 : row.soldQuantity();
                    return new AdminCatalogDtos.ProductPerformance(product.getId(), product.title().en(), total, sold,
                            Amounts.ratioPercent(BigDecimal.valueOf(sold), BigDecimal.valueOf(total)).toPlainString());
                })
                .toList();
        return new AdminCatalogDtos.PartnerPerformance(partnerId, partner.name(), from, to, ((Number) sales[0]).longValue(),
                Amounts.scale((BigDecimal) sales[1]), ((Number) sales[2]).longValue(), issued, redeemed,
                Amounts.ratioPercent(BigDecimal.valueOf(redeemed), BigDecimal.valueOf(issued)).toPlainString(), productRows);
    }
}
