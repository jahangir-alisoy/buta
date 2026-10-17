package az.buta.admin;

import az.buta.audit.AuditLogRepository;
import az.buta.catalog.OfferPricingService;
import az.buta.catalog.PricingContext;
import az.buta.catalog.domain.Offer;
import az.buta.catalog.domain.OfferRepository;
import az.buta.common.money.Amounts;
import az.buta.common.web.PageResponse;
import az.buta.pricing.LoyaltySettings;
import az.buta.pricing.PricingSettings;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminInsightsService {

    private final OfferRepository offers;
    private final OfferPricingService pricing;
    private final AuditLogRepository auditLogs;
    private final EntityManager entityManager;

    public AdminInsightsService(OfferRepository offers, OfferPricingService pricing, AuditLogRepository auditLogs, EntityManager entityManager) {
        this.offers = offers;
        this.pricing = pricing;
        this.auditLogs = auditLogs;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public AdminDtos.ImpactPreview pricingImpact(PricingSettings proposed) {
        PricingContext current = pricing.context();
        return impact(new PricingContext(current.now(), proposed, current.loyalty(), current.highestTierMultiplier(),
                current.campaignBonusByOffer(), current.campaignByOffer()));
    }

    @Transactional(readOnly = true)
    public AdminDtos.ImpactPreview loyaltyImpact(LoyaltySettings proposed) {
        PricingContext current = pricing.context();
        return impact(new PricingContext(current.now(), current.pricing(), proposed, current.highestTierMultiplier(),
                current.campaignBonusByOffer(), current.campaignByOffer()));
    }

    private AdminDtos.ImpactPreview impact(PricingContext context) {
        List<Offer> onSale = offers.findOnSale(context.now());
        List<String> failing = onSale.stream().filter(offer -> !pricing.breakdown(offer, context).sellable()).map(Offer::slug).toList();
        return new AdminDtos.ImpactPreview(onSale.size(), failing);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminDtos.AuditView> auditLogs(String action, String targetType, int page, int size) {
        return PageResponse.of(auditLogs.search(blankToNull(action), blankToNull(targetType), PageRequest.of(page, size)),
                log -> new AdminDtos.AuditView(log.getId(), log.actorUserId(), log.actorType().name(), log.action(), log.targetType(),
                        log.targetId(), log.details(), log.requestId(), log.createdAt()));
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.VariantMetrics> recommendationMetrics(Instant from, Instant to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "with served as (select request_id, variant, count(*) impressions from recommendation_impressions "
                                + "where served_at >= :from and served_at < :to group by request_id, variant), "
                                + "clicks as (select recommendation_request_id request_id, count(*) clicks from interaction_events "
                                + "where event_type = 'RECOMMENDATION_CLICK' and recommendation_request_id is not null group by recommendation_request_id), "
                                + "carts as (select recommendation_request_id request_id, count(*) carts from interaction_events "
                                + "where event_type = 'CART_ADD' and recommendation_request_id is not null group by recommendation_request_id), "
                                + "purchases as (select i.recommendation_request_id request_id, count(*) purchases, sum(i.cash_paid) revenue, "
                                + "sum(i.contribution_margin) margin from order_items i join orders o on o.id = i.order_id "
                                + "where i.recommendation_request_id is not null and o.status in ('FULFILLED', 'COMPLETED', 'PARTIALLY_REFUNDED') "
                                + "and i.refunded_quantity = 0 group by i.recommendation_request_id) "
                                + "select s.variant, sum(s.impressions), coalesce(sum(c.clicks), 0), coalesce(sum(k.carts), 0), "
                                + "coalesce(sum(p.purchases), 0), coalesce(sum(p.revenue), 0), coalesce(sum(p.margin), 0) from served s "
                                + "left join clicks c on c.request_id = s.request_id left join carts k on k.request_id = s.request_id "
                                + "left join purchases p on p.request_id = s.request_id group by s.variant order by s.variant")
                .setParameter("from", from).setParameter("to", to)
                .getResultList();
        List<AdminDtos.VariantMetrics> metrics = new ArrayList<>();
        for (Object[] row : rows) {
            long impressions = ((Number) row[1]).longValue();
            long clicks = ((Number) row[2]).longValue();
            long purchases = ((Number) row[4]).longValue();
            metrics.add(new AdminDtos.VariantMetrics((String) row[0], impressions, clicks, ((Number) row[3]).longValue(), purchases,
                    Amounts.scale(new BigDecimal(row[5].toString())), Amounts.scale(new BigDecimal(row[6].toString())),
                    Amounts.ratioPercent(BigDecimal.valueOf(clicks), BigDecimal.valueOf(impressions)).toPlainString(),
                    Amounts.ratioPercent(BigDecimal.valueOf(purchases), BigDecimal.valueOf(impressions)).toPlainString()));
        }
        return metrics;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
