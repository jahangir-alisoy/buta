package az.buta.catalog;

import az.buta.catalog.domain.Category;
import az.buta.catalog.domain.CategoryRepository;
import az.buta.catalog.domain.Offer;
import az.buta.catalog.domain.OfferRepository;
import az.buta.catalog.domain.OfferType;
import az.buta.catalog.domain.Partner;
import az.buta.catalog.domain.PartnerRepository;
import az.buta.catalog.dto.CampaignSectionDto;
import az.buta.catalog.dto.CategoryDto;
import az.buta.catalog.dto.ComponentDto;
import az.buta.catalog.dto.HomeDto;
import az.buta.catalog.dto.OfferDetailDto;
import az.buta.catalog.dto.OfferSearchCriteria;
import az.buta.catalog.dto.OfferSort;
import az.buta.catalog.dto.PartnerDetailDto;
import az.buta.catalog.dto.PartnerSummaryDto;
import az.buta.catalog.dto.OfferCardDto;
import az.buta.catalog.domain.ActiveStatus;
import az.buta.common.error.ApiException;
import az.buta.common.text.SearchKeys;
import az.buta.common.web.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class CatalogQueryService {

    private static final int SECTION_SIZE = 8;

    private final OfferCatalog catalog;
    private final OfferViewAssembler assembler;
    private final OfferPopularityService popularity;
    private final CategoryRepository categories;
    private final PartnerRepository partners;
    private final OfferRepository offers;
    private final Clock clock;

    public CatalogQueryService(OfferCatalog catalog, OfferViewAssembler assembler, OfferPopularityService popularity,
                               CategoryRepository categories, PartnerRepository partners, OfferRepository offers, Clock clock) {
        this.catalog = catalog;
        this.assembler = assembler;
        this.popularity = popularity;
        this.categories = categories;
        this.partners = partners;
        this.offers = offers;
        this.clock = clock;
    }

    public PageResponse<OfferCardDto> search(OfferSearchCriteria criteria, Viewer viewer) {
        Optional<UUID> categoryId = Optional.ofNullable(criteria.category()).filter(value -> !value.isBlank())
                .map(slug -> categories.findBySlug(slug).map(Category::getId).orElse(new UUID(0, 0)));
        List<String> tokens = criteria.q() == null ? List.of()
                : Arrays.stream(SearchKeys.fold(criteria.q()).split(" ")).filter(token -> !token.isBlank()).toList();
        Predicate<OfferSnapshot> filter = snapshot -> snapshot.sellable()
                && (criteria.includeSoldOut() || snapshot.available() > 0)
                && tokens.stream().allMatch(token -> snapshot.searchKey().contains(token))
                && categoryId.map(id -> snapshot.categoryIds().contains(id)).orElse(true)
                && (criteria.offerType() == null || snapshot.type() == criteria.offerType())
                && (criteria.productType() == null || snapshot.productTypes().contains(criteria.productType()))
                && (criteria.partner() == null || snapshot.partners().stream().anyMatch(partner -> partner.slug().equals(criteria.partner())))
                && (criteria.city() == null || snapshot.cities().stream().anyMatch(city -> city.equalsIgnoreCase(criteria.city())))
                && (criteria.minPrice() == null || snapshot.sellingPrice().compareTo(criteria.minPrice()) >= 0)
                && (criteria.maxPrice() == null || snapshot.sellingPrice().compareTo(criteria.maxPrice()) <= 0)
                && (criteria.eventFrom() == null || (snapshot.eventStartsAt() != null && !snapshot.eventStartsAt().isBefore(criteria.eventFrom())))
                && (criteria.eventTo() == null || (snapshot.eventStartsAt() != null && !snapshot.eventStartsAt().isAfter(criteria.eventTo())))
                && (!criteria.plusOnly() || snapshot.plusOnly());
        List<OfferSnapshot> matches = catalog.onSale().stream()
                .filter(filter)
                .sorted(comparator(criteria.sort() == null ? OfferSort.RECOMMENDED : criteria.sort()))
                .toList();
        int from = Math.min(criteria.page() * criteria.size(), matches.size());
        int to = Math.min(from + criteria.size(), matches.size());
        int totalPages = (int) Math.ceil(matches.size() / (double) criteria.size());
        return new PageResponse<>(assembler.cards(matches.subList(from, to), viewer), criteria.page(), criteria.size(),
                matches.size(), totalPages);
    }

    @Transactional(readOnly = true)
    public OfferDetailDto detail(String slug, Viewer viewer) {
        Instant now = clock.instant();
        Offer offer = offers.findBySlug(slug).filter(candidate -> candidate.isOnSaleAt(now)).orElseThrow(ApiException::notFound);
        OfferSnapshot snapshot = catalog.build(List.of(offer)).getFirst();
        Locale locale = viewer.locale();
        List<ComponentDto> components = offer.components().stream()
                .map(component -> assembler.component(component.product(), component.quantity(), locale))
                .toList();
        Instant validFrom = offer.components().stream().map(component -> component.product().validFrom())
                .max(Comparator.naturalOrder()).orElse(offer.saleStartsAt());
        return new OfferDetailDto(assembler.card(snapshot, viewer), offer.description().in(locale), offer.tags(), components,
                validFrom, snapshot.validUntil(), offer.maxPerOrder());
    }

    public HomeDto home(Viewer viewer) {
        Instant now = clock.instant();
        List<OfferSnapshot> visible = catalog.onSale().stream().filter(OfferSnapshot::sellable).toList();
        List<OfferSnapshot> available = visible.stream().filter(snapshot -> snapshot.available() > 0).toList();
        CampaignSectionDto campaign = available.stream()
                .map(OfferSnapshot::campaign)
                .filter(Objects::nonNull)
                .filter(CampaignRef::featured)
                .findFirst()
                .map(ref -> new CampaignSectionDto(ref.id(), ref.slug(), ref.name().in(viewer.locale()), ref.bannerText().in(viewer.locale()),
                        ref.audience(), ref.bonusPointsPerUnit(), ref.endsAt(),
                        assembler.cards(available.stream().filter(snapshot -> snapshot.campaign() != null
                                && snapshot.campaign().id().equals(ref.id())).limit(SECTION_SIZE).toList(), viewer)))
                .orElse(null);
        List<OfferSnapshot> popular = available.stream().sorted(comparator(OfferSort.RECOMMENDED)).limit(SECTION_SIZE).toList();
        List<OfferSnapshot> bundles = available.stream().filter(snapshot -> snapshot.type() == OfferType.BUNDLE)
                .sorted(comparator(OfferSort.SAVINGS_DESC)).limit(SECTION_SIZE).toList();
        List<OfferSnapshot> thisWeek = available.stream()
                .filter(snapshot -> snapshot.eventStartsAt() != null && snapshot.eventStartsAt().isBefore(now.plus(Duration.ofDays(7))))
                .sorted(comparator(OfferSort.EVENT_DATE)).limit(SECTION_SIZE).toList();
        List<OfferSnapshot> endingSoon = available.stream()
                .filter(snapshot -> snapshot.endsAt().isBefore(now.plus(Duration.ofDays(10))))
                .sorted(comparator(OfferSort.ENDING_SOON)).limit(SECTION_SIZE).toList();
        return new HomeDto(campaign, assembler.cards(popular, viewer), assembler.cards(bundles, viewer), assembler.cards(thisWeek, viewer),
                assembler.cards(endingSoon, viewer), categories(viewer.locale()));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> categories(Locale locale) {
        List<OfferSnapshot> visible = catalog.onSale().stream().filter(snapshot -> snapshot.sellable() && snapshot.available() > 0).toList();
        return categories.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(category -> new CategoryDto(category.getId(), category.slug(), category.name().in(locale), category.iconKey(),
                        (int) visible.stream().filter(snapshot -> snapshot.categoryIds().contains(category.getId())).count()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PartnerSummaryDto> partners(Locale locale) {
        List<OfferSnapshot> visible = catalog.onSale().stream().filter(OfferSnapshot::sellable).toList();
        return partners.findByStatusOrderByNameAsc(ActiveStatus.ACTIVE).stream()
                .map(partner -> new PartnerSummaryDto(partner.getId(), partner.slug(), partner.name(), partner.description().in(locale),
                        partner.city(), partner.demo(), partner.logoPath(),
                        (int) visible.stream().filter(snapshot -> snapshot.partners().stream()
                                .anyMatch(ref -> ref.id().equals(partner.getId()))).count()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PartnerDetailDto partner(String slug, Viewer viewer) {
        Partner partner = partners.findBySlug(slug).filter(Partner::isActive).orElseThrow(ApiException::notFound);
        List<OfferSnapshot> partnerOffers = catalog.onSale().stream()
                .filter(snapshot -> snapshot.sellable() && snapshot.partners().stream().anyMatch(ref -> ref.id().equals(partner.getId())))
                .sorted(comparator(OfferSort.RECOMMENDED))
                .toList();
        return new PartnerDetailDto(partner.getId(), partner.slug(), partner.name(), partner.description().in(viewer.locale()),
                partner.city(), partner.address(), partner.websiteUrl(), partner.demo(), partner.logoPath(),
                assembler.cards(partnerOffers, viewer));
    }

    private Comparator<OfferSnapshot> comparator(OfferSort sort) {
        Comparator<OfferSnapshot> byId = Comparator.comparing(snapshot -> snapshot.id().toString());
        return switch (sort) {
            case PRICE_ASC -> Comparator.comparing(OfferSnapshot::sellingPrice).thenComparing(byId);
            case PRICE_DESC -> Comparator.comparing(OfferSnapshot::sellingPrice).reversed().thenComparing(byId);
            case SAVINGS_DESC -> Comparator.comparing((OfferSnapshot snapshot) -> snapshot.pricing().savingsPercent()).reversed().thenComparing(byId);
            case ENDING_SOON -> Comparator.comparing(OfferSnapshot::endsAt).thenComparing(byId);
            case NEWEST -> Comparator.comparing((OfferSnapshot snapshot) -> Optional.ofNullable(snapshot.publishedAt()).orElse(Instant.EPOCH))
                    .reversed().thenComparing(byId);
            case EVENT_DATE -> Comparator.comparing((OfferSnapshot snapshot) -> Optional.ofNullable(snapshot.eventStartsAt()).orElse(Instant.MAX))
                    .thenComparing(byId);
            case RECOMMENDED -> popularityComparator().thenComparing(byId);
        };
    }

    private Comparator<OfferSnapshot> popularityComparator() {
        Map<UUID, Long> counts = popularity.fulfilledOrderCounts();
        return Comparator.comparing((OfferSnapshot snapshot) -> counts.getOrDefault(snapshot.id(), 0L)).reversed()
                .thenComparing(OfferSnapshot::featured, Comparator.reverseOrder())
                .thenComparing(OfferSnapshot::businessPriority, Comparator.reverseOrder())
                .thenComparing((OfferSnapshot snapshot) -> snapshot.pricing().savingsPercent(), Comparator.reverseOrder());
    }
}
