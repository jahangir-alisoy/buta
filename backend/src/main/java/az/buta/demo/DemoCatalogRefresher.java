package az.buta.demo;

import az.buta.catalog.OfferCatalog;
import az.buta.catalog.domain.CampaignRepository;
import az.buta.catalog.domain.Offer;
import az.buta.catalog.domain.OfferRepository;
import az.buta.catalog.domain.PartnerProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoCatalogRefresher {

    private static final Logger LOG = LoggerFactory.getLogger(DemoCatalogRefresher.class);
    private static final Duration STALE_AFTER = Duration.ofDays(14);
    private static final Duration TARGET_AGE = Duration.ofDays(3);

    private final PartnerProductRepository products;
    private final OfferRepository offers;
    private final CampaignRepository campaigns;
    private final OfferCatalog catalog;
    private final Clock clock;

    public DemoCatalogRefresher(PartnerProductRepository products, OfferRepository offers, CampaignRepository campaigns,
                                OfferCatalog catalog, Clock clock) {
        this.products = products;
        this.offers = offers;
        this.campaigns = campaigns;
        this.catalog = catalog;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        refresh();
    }

    @Transactional
    public void refresh() {
        Instant now = clock.instant();
        List<Offer> demoOffers = offers.findAll().stream()
                .filter(offer -> !offer.components().isEmpty()
                        && offer.components().stream().allMatch(component -> component.product().partner().demo()))
                .toList();
        Instant oldestStart = demoOffers.stream().map(Offer::saleStartsAt).min(Comparator.naturalOrder()).orElse(now);
        if (!oldestStart.isBefore(now.minus(STALE_AFTER))) {
            return;
        }
        Duration shift = Duration.ofDays(Duration.between(oldestStart, now.minus(TARGET_AGE)).toDays());
        products.findAll().stream().filter(product -> product.partner().demo()).forEach(product -> product.shiftDates(shift, now));
        demoOffers.forEach(offer -> offer.shiftSaleWindow(shift, now));
        campaigns.findAll().stream().filter(campaign -> campaign.offers().stream().anyMatch(demoOffers::contains))
                .forEach(campaign -> campaign.shift(shift, now));
        catalog.evict();
        LOG.info("Demo catalog dates moved forward by {} days", shift.toDays());
    }
}
