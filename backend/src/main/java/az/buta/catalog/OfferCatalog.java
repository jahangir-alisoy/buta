package az.buta.catalog;

import az.buta.catalog.domain.Category;
import az.buta.catalog.domain.Offer;
import az.buta.catalog.domain.OfferComponent;
import az.buta.catalog.domain.OfferRepository;
import az.buta.catalog.domain.PartnerProduct;
import az.buta.catalog.domain.ProductType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OfferCatalog {

    private static final String ON_SALE = "on-sale";

    private final OfferRepository offers;
    private final InventoryService inventory;
    private final OfferPricingService pricing;
    private final TransactionTemplate readOnly;
    private final Cache<String, List<OfferSnapshot>> cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(15)).build();

    public OfferCatalog(OfferRepository offers, InventoryService inventory, OfferPricingService pricing,
                        PlatformTransactionManager transactionManager) {
        this.offers = offers;
        this.inventory = inventory;
        this.pricing = pricing;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    public List<OfferSnapshot> onSale() {
        return cache.get(ON_SALE, ignored -> readOnly.execute(status -> {
            PricingContext context = pricing.context();
            return List.copyOf(build(offers.findOnSale(context.now()), context));
        }));
    }

    public void evict() {
        cache.invalidateAll();
    }

    @Transactional(readOnly = true)
    public Optional<OfferSnapshot> bySlug(String slug) {
        return offers.findBySlug(slug).map(offer -> build(List.of(offer)).getFirst());
    }

    @Transactional(readOnly = true)
    public Map<UUID, OfferSnapshot> byIds(Collection<UUID> ids) {
        return build(offers.findAllById(ids)).stream().collect(Collectors.toMap(OfferSnapshot::id, snapshot -> snapshot));
    }

    @Transactional(readOnly = true)
    public List<OfferSnapshot> build(List<Offer> source) {
        return build(source, pricing.context());
    }

    private List<OfferSnapshot> build(List<Offer> source, PricingContext context) {
        Set<UUID> productIds = source.stream()
                .flatMap(offer -> offer.components().stream())
                .map(component -> component.product().getId())
                .collect(Collectors.toSet());
        Map<UUID, Integer> availability = inventory.availability(productIds);
        List<OfferSnapshot> snapshots = new ArrayList<>();
        for (Offer offer : source) {
            snapshots.add(snapshot(offer, availability, context));
        }
        return snapshots;
    }

    private OfferSnapshot snapshot(Offer offer, Map<UUID, Integer> availability, PricingContext context) {
        Instant now = context.now();
        List<OfferComponent> components = offer.components();
        boolean usable = !components.isEmpty() && components.stream().allMatch(component -> component.product().isUsableAt(now));
        int available = usable ? components.stream()
                .mapToInt(component -> availability.getOrDefault(component.product().getId(), 0) / component.quantity())
                .min()
                .orElse(0) : 0;
        Instant validUntil = components.stream().map(component -> component.product().validUntil())
                .min(Comparator.naturalOrder()).orElse(offer.saleEndsAt());
        Instant eventStartsAt = components.stream().map(component -> component.product().eventStartsAt())
                .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        Set<OfferSnapshot.PartnerRef> partners = new LinkedHashSet<>();
        Set<String> cities = new LinkedHashSet<>();
        Set<ProductType> types = new LinkedHashSet<>();
        for (OfferComponent component : components) {
            PartnerProduct product = component.product();
            partners.add(new OfferSnapshot.PartnerRef(product.partner().getId(), product.partner().slug(), product.partner().name(),
                    product.partner().demo()));
            cities.add(product.city());
            types.add(product.productType());
        }
        Category category = offer.primaryCategory();
        return new OfferSnapshot(offer.getId(), offer.slug(), offer.offerType(), offer.visibility(),
                new OfferSnapshot.CategoryRef(category.getId(), category.slug(), category.name(), category.iconKey()),
                offer.categoryIds(), List.copyOf(partners), offer.title(), offer.subtitle(), offer.imagePath(),
                pricing.breakdown(offer, context), usable && offer.isOnSaleAt(now), available, offer.maxPerOrder(),
                offer.saleEndsAt(), validUntil, eventStartsAt, cities, types,
                offer.searchKeyValue(), offer.businessPriority(), offer.featured(), offer.publishedAt(),
                context.bonusFor(offer.getId()), context.campaignByOffer().get(offer.getId()), components.size());
    }
}
