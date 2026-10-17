package az.buta.catalog.admin;

import az.buta.audit.AuditService;
import az.buta.catalog.InventoryService;
import az.buta.catalog.OfferCatalog;
import az.buta.catalog.OfferPricingService;
import az.buta.catalog.PricingContext;
import az.buta.catalog.domain.Campaign;
import az.buta.catalog.domain.CampaignRepository;
import az.buta.catalog.domain.Category;
import az.buta.catalog.domain.CategoryRepository;
import az.buta.catalog.domain.CostModel;
import az.buta.catalog.domain.Offer;
import az.buta.catalog.domain.OfferComponent;
import az.buta.catalog.domain.OfferRepository;
import az.buta.catalog.domain.OfferStatus;
import az.buta.catalog.domain.OfferType;
import az.buta.catalog.domain.Partner;
import az.buta.catalog.domain.PartnerProduct;
import az.buta.catalog.domain.PartnerProductRepository;
import az.buta.catalog.domain.PartnerRepository;
import az.buta.catalog.domain.ProductInventory;
import az.buta.common.error.ApiException;
import az.buta.common.error.ErrorCode;
import az.buta.common.text.SafeUrls;
import az.buta.common.text.Slugs;
import az.buta.pricing.PriceBreakdown;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminCatalogService {

    private final CategoryRepository categories;
    private final PartnerRepository partners;
    private final PartnerProductRepository products;
    private final OfferRepository offers;
    private final CampaignRepository campaigns;
    private final InventoryService inventory;
    private final OfferPricingService pricing;
    private final OfferCatalog catalog;
    private final AuditService audit;
    private final Clock clock;

    public AdminCatalogService(CategoryRepository categories, PartnerRepository partners, PartnerProductRepository products,
                               OfferRepository offers, CampaignRepository campaigns, InventoryService inventory, OfferPricingService pricing,
                               OfferCatalog catalog, AuditService audit, Clock clock) {
        this.categories = categories;
        this.partners = partners;
        this.products = products;
        this.offers = offers;
        this.campaigns = campaigns;
        this.inventory = inventory;
        this.pricing = pricing;
        this.catalog = catalog;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminCatalogDtos.CategoryView> categories() {
        return categories.findAllByOrderBySortOrderAsc().stream().map(AdminCatalogService::view).toList();
    }

    @Transactional
    public AdminCatalogDtos.CategoryView createCategory(UUID adminId, CategoryRequest request) {
        String slug = request.slug() == null ? Slugs.from(request.name().en()) : request.slug();
        if (categories.existsBySlug(slug)) {
            throw ApiException.field(ErrorCode.CONFLICT, "slug");
        }
        Category category = categories.save(Category.create(slug, request.name(), request.iconKey(), request.sortOrder(), clock.instant()));
        category.update(request.name(), request.iconKey(), request.sortOrder(), request.active(), clock.instant());
        audit.admin(adminId, "CATEGORY_CREATED", "CATEGORY", category.getId(), Map.of("slug", slug));
        catalog.evict();
        return view(category);
    }

    @Transactional
    public AdminCatalogDtos.CategoryView updateCategory(UUID adminId, UUID id, CategoryRequest request) {
        Category category = categories.findById(id).orElseThrow(ApiException::notFound);
        category.update(request.name(), request.iconKey(), request.sortOrder(), request.active(), clock.instant());
        audit.admin(adminId, "CATEGORY_UPDATED", "CATEGORY", id, Map.of("active", request.active()));
        catalog.evict();
        return view(category);
    }

    @Transactional(readOnly = true)
    public List<AdminCatalogDtos.PartnerView> partners() {
        return partners.findAllByOrderByNameAsc().stream().map(AdminCatalogService::view).toList();
    }

    @Transactional
    public AdminCatalogDtos.PartnerView createPartner(UUID adminId, PartnerRequest request) {
        validatePartnerUrls(request);
        String slug = request.slug() == null ? Slugs.from(request.name()) : request.slug();
        if (partners.existsBySlug(slug)) {
            throw ApiException.field(ErrorCode.CONFLICT, "slug");
        }
        Partner partner = Partner.create(slug, clock.instant());
        applyPartner(partner, request);
        partners.save(partner);
        audit.admin(adminId, "PARTNER_CREATED", "PARTNER", partner.getId(), Map.of("slug", slug));
        return view(partner);
    }

    @Transactional
    public AdminCatalogDtos.PartnerView updatePartner(UUID adminId, UUID id, PartnerRequest request) {
        validatePartnerUrls(request);
        Partner partner = partners.findById(id).orElseThrow(ApiException::notFound);
        applyPartner(partner, request);
        audit.admin(adminId, "PARTNER_UPDATED", "PARTNER", id, Map.of("status", request.status()));
        catalog.evict();
        return view(partner);
    }

    @Transactional(readOnly = true)
    public List<AdminCatalogDtos.ProductView> products() {
        List<PartnerProduct> all = products.findAllWithRelations();
        Map<UUID, ProductInventory> stock = inventory.snapshot(all.stream().map(PartnerProduct::getId).toList());
        return all.stream().map(product -> view(product, stock.get(product.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public AdminCatalogDtos.ProductView product(UUID id) {
        PartnerProduct product = products.findById(id).orElseThrow(ApiException::notFound);
        return view(product, inventory.snapshot(List.of(id)).get(id));
    }

    @Transactional
    public AdminCatalogDtos.ProductView createProduct(UUID adminId, ProductRequest request) {
        validateProduct(request);
        Partner partner = partners.findById(request.partnerId()).orElseThrow(() -> ApiException.field(ErrorCode.VALIDATION_FAILED, "partnerId"));
        PartnerProduct product = PartnerProduct.create(partner, clock.instant());
        applyProduct(product, request);
        products.save(product);
        inventory.create(product.getId(), request.initialInventory() == null ? 0 : request.initialInventory());
        audit.admin(adminId, "PRODUCT_CREATED", "PRODUCT", product.getId(), Map.of("partner", partner.slug()));
        return product(product.getId());
    }

    @Transactional
    public AdminCatalogDtos.ProductView updateProduct(UUID adminId, UUID id, ProductRequest request) {
        validateProduct(request);
        PartnerProduct product = products.findById(id).orElseThrow(ApiException::notFound);
        if (!product.partner().getId().equals(request.partnerId())) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "partnerId");
        }
        applyProduct(product, request);
        products.flush();
        List<String> failing = publishedOffersFailingMargin(offers.findContainingProducts(List.of(id)), pricing.context());
        if (!failing.isEmpty()) {
            throw ApiException.withProperties(ErrorCode.MARGIN_RULE_FAILED, Map.of("offers", failing));
        }
        audit.admin(adminId, "PRODUCT_UPDATED", "PRODUCT", id, Map.of("retailPrice", request.retailPrice(), "costModel", request.costModel()));
        catalog.evict();
        return product(id);
    }

    @Transactional
    public AdminCatalogDtos.ProductView adjustInventory(UUID adminId, UUID productId, InventoryAdjustmentRequest request) {
        products.findById(productId).orElseThrow(ApiException::notFound);
        inventory.adjust(productId, request.delta(), request.reason(), adminId);
        audit.admin(adminId, "INVENTORY_ADJUSTED", "PRODUCT", productId, Map.of("delta", request.delta(), "reason", request.reason()));
        catalog.evict();
        return product(productId);
    }

    @Transactional(readOnly = true)
    public List<AdminCatalogDtos.OfferView> offers() {
        PricingContext context = pricing.context();
        List<Offer> all = offers.findAll();
        Map<UUID, Integer> availability = catalog.build(all).stream()
                .collect(Collectors.toMap(snapshot -> snapshot.id(), snapshot -> snapshot.available()));
        return all.stream()
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .map(offer -> view(offer, pricing.breakdown(offer, context), availability.getOrDefault(offer.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminCatalogDtos.OfferView offer(UUID id) {
        Offer offer = offers.findById(id).orElseThrow(ApiException::notFound);
        int available = catalog.build(List.of(offer)).getFirst().available();
        return view(offer, pricing.breakdown(offer, pricing.context()), available);
    }

    @Transactional
    public AdminCatalogDtos.OfferView createOffer(UUID adminId, OfferRequest request) {
        String slug = request.slug() == null ? Slugs.from(request.title().en()) : request.slug();
        if (offers.existsBySlug(slug)) {
            throw ApiException.field(ErrorCode.CONFLICT, "slug");
        }
        Offer offer = Offer.draft(slug, clock.instant());
        applyOffer(offer, request);
        offers.save(offer);
        audit.admin(adminId, "OFFER_CREATED", "OFFER", offer.getId(), Map.of("slug", slug));
        return offer(offer.getId());
    }

    @Transactional
    public AdminCatalogDtos.OfferView updateOffer(UUID adminId, UUID id, OfferRequest request) {
        Offer offer = offers.findById(id).orElseThrow(ApiException::notFound);
        applyOffer(offer, request);
        if (offer.status() == OfferStatus.PUBLISHED) {
            List<String> failures = publishFailures(offer);
            if (!failures.isEmpty()) {
                throw ApiException.withProperties(ErrorCode.PUBLISH_RULE_FAILED, Map.of("failures", failures));
            }
        }
        audit.admin(adminId, "OFFER_UPDATED", "OFFER", id, Map.of("sellingPrice", request.sellingPrice(), "status", offer.status()));
        catalog.evict();
        return offer(id);
    }

    @Transactional
    public AdminCatalogDtos.OfferView changeStatus(UUID adminId, UUID id, OfferStatus target) {
        Offer offer = offers.findById(id).orElseThrow(ApiException::notFound);
        Instant now = clock.instant();
        if (target == OfferStatus.PUBLISHED) {
            List<String> failures = publishFailures(offer);
            if (!failures.isEmpty()) {
                throw ApiException.withProperties(ErrorCode.PUBLISH_RULE_FAILED, Map.of("failures", failures));
            }
            offer.publish(now);
        } else {
            offer.changeStatus(target, now);
        }
        audit.admin(adminId, "OFFER_STATUS_CHANGED", "OFFER", id, Map.of("status", target));
        catalog.evict();
        return offer(id);
    }

    @Transactional(readOnly = true)
    public PriceBreakdown marginPreview(MarginPreviewRequest request) {
        PricingContext context = pricing.context();
        List<OfferPricingService.ProductQuantity> items = request.components().stream()
                .map(component -> new OfferPricingService.ProductQuantity(
                        products.findById(component.productId()).orElseThrow(() -> ApiException.field(ErrorCode.VALIDATION_FAILED, "components")),
                        component.quantity(), component.negotiatedUnitCost()))
                .toList();
        int bonus = request.offerId() == null ? 0 : context.bonusFor(request.offerId());
        return pricing.breakdownFor(request.sellingPrice(), items, bonus, context);
    }

    @Transactional(readOnly = true)
    public List<AdminCatalogDtos.CampaignView> campaigns() {
        return campaigns.findAllWithOffers().stream().map(AdminCatalogService::view).toList();
    }

    @Transactional
    public AdminCatalogDtos.CampaignView createCampaign(UUID adminId, CampaignRequest request) {
        String slug = request.slug() == null ? Slugs.from(request.name().en()) : request.slug();
        if (campaigns.existsBySlug(slug)) {
            throw ApiException.field(ErrorCode.CONFLICT, "slug");
        }
        Campaign campaign = Campaign.create(slug, clock.instant());
        applyCampaign(campaign, request);
        campaigns.save(campaign);
        audit.admin(adminId, "CAMPAIGN_CREATED", "CAMPAIGN", campaign.getId(), Map.of("slug", slug));
        catalog.evict();
        return view(campaign);
    }

    @Transactional
    public AdminCatalogDtos.CampaignView updateCampaign(UUID adminId, UUID id, CampaignRequest request) {
        Campaign campaign = campaigns.findById(id).orElseThrow(ApiException::notFound);
        applyCampaign(campaign, request);
        audit.admin(adminId, "CAMPAIGN_UPDATED", "CAMPAIGN", id, Map.of("bonusPointsPerUnit", request.bonusPointsPerUnit()));
        catalog.evict();
        return view(campaign);
    }

    private void applyCampaign(Campaign campaign, CampaignRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "endsAt");
        }
        Set<Offer> selected = new HashSet<>(offers.findAllById(request.offerIds()));
        if (selected.size() != request.offerIds().size()) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "offerIds");
        }
        PricingContext context = pricing.context();
        List<String> failing = selected.stream()
                .filter(offer -> offer.status() == OfferStatus.PUBLISHED)
                .filter(offer -> !pricing.breakdown(offer.sellingPrice(), offer.components(),
                        Math.max(request.bonusPointsPerUnit(), context.bonusFor(offer.getId())), context).sellable())
                .map(Offer::slug)
                .toList();
        if (!failing.isEmpty()) {
            throw ApiException.withProperties(ErrorCode.MARGIN_RULE_FAILED, Map.of("offers", failing));
        }
        campaign.update(request.name(), request.bannerText(), request.audience(), request.startsAt(), request.endsAt(),
                request.bonusPointsPerUnit(), request.featured(), request.status(), selected, clock.instant());
    }

    private List<String> publishFailures(Offer offer) {
        Instant now = clock.instant();
        List<String> failures = new ArrayList<>();
        List<OfferComponent> components = offer.components();
        if (offer.offerType() == OfferType.SINGLE && components.size() != 1) {
            failures.add("SINGLE_REQUIRES_ONE_COMPONENT");
        }
        if (offer.offerType() == OfferType.BUNDLE && components.size() < 2) {
            failures.add("BUNDLE_REQUIRES_TWO_COMPONENTS");
        }
        if (components.stream().anyMatch(component -> !component.product().isUsableAt(now))) {
            failures.add("COMPONENT_NOT_USABLE");
        }
        if (!offer.saleEndsAt().isAfter(now)) {
            failures.add("SALE_WINDOW_ENDED");
        }
        if (!SafeUrls.isSafeImagePath(offer.imagePath())) {
            failures.add("IMAGE_PATH_NOT_ALLOWED");
        }
        PriceBreakdown breakdown = pricing.breakdown(offer, pricing.context());
        breakdown.rules().stream().filter(rule -> !rule.passed()).forEach(rule -> failures.add(rule.code()));
        return failures;
    }

    private List<String> publishedOffersFailingMargin(List<Offer> candidates, PricingContext context) {
        return candidates.stream()
                .filter(offer -> offer.status() == OfferStatus.PUBLISHED)
                .filter(offer -> !pricing.breakdown(offer, context).sellable())
                .map(Offer::slug)
                .toList();
    }

    private void applyOffer(Offer offer, OfferRequest request) {
        if (!request.saleEndsAt().isAfter(request.saleStartsAt())) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "saleEndsAt");
        }
        if (!SafeUrls.isSafeImagePath(request.imagePath())) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "imagePath");
        }
        Category category = categories.findById(request.primaryCategoryId())
                .orElseThrow(() -> ApiException.field(ErrorCode.VALIDATION_FAILED, "primaryCategoryId"));
        Set<UUID> seen = new HashSet<>();
        List<Offer.ComponentSpec> specs = new ArrayList<>();
        for (ComponentRequest component : request.components()) {
            if (!seen.add(component.productId())) {
                throw ApiException.field(ErrorCode.VALIDATION_FAILED, "components");
            }
            PartnerProduct product = products.findById(component.productId())
                    .orElseThrow(() -> ApiException.field(ErrorCode.VALIDATION_FAILED, "components"));
            specs.add(new Offer.ComponentSpec(product, component.quantity(), component.negotiatedUnitCost()));
        }
        Instant now = clock.instant();
        offer.describe(request.offerType(), category, request.title(), request.subtitle(), request.description(), request.imagePath(),
                request.tags() == null ? List.of() : request.tags(), now);
        offer.configureSale(request.sellingPrice(), request.visibility(), request.saleStartsAt(), request.saleEndsAt(), request.maxPerOrder(),
                request.businessPriority(), request.featured(), now);
        offer.replaceComponents(specs, now);
        offer.refreshSearchKey();
    }

    private void applyProduct(PartnerProduct product, ProductRequest request) {
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> ApiException.field(ErrorCode.VALIDATION_FAILED, "categoryId"));
        Instant now = clock.instant();
        product.describe(category, request.productType(), request.title(), request.description(), request.terms(),
                request.redemptionInstructions(), request.eligibilityNote(), request.redemptionMethod(), now);
        product.price(request.retailPrice(), request.costModel(), request.unitNetCost(), request.commissionPercent(), now);
        product.schedule(request.validFrom(), request.validUntil(), request.eventStartsAt(), request.eventEndsAt(), request.venueName(),
                request.venueAddress(), request.city(), now);
        product.refundPolicy(request.refundable(), request.refundDeadlineHours(), request.restockOnRefund(), request.status(), now);
    }

    private void applyPartner(Partner partner, PartnerRequest request) {
        partner.update(request.name().strip(), request.description(), request.city().strip(), request.address(), request.websiteUrl(),
                request.logoPath(), request.demo(), request.status(), clock.instant());
    }

    private static void validatePartnerUrls(PartnerRequest request) {
        if (!SafeUrls.isHttps(request.websiteUrl())) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "websiteUrl");
        }
        if (!SafeUrls.isSafeImagePath(request.logoPath())) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "logoPath");
        }
    }

    private static void validateProduct(ProductRequest request) {
        if (!request.validUntil().isAfter(request.validFrom())) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "validUntil");
        }
        if (request.costModel() == CostModel.WHOLESALE && request.unitNetCost() == null) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "unitNetCost");
        }
        if (request.costModel() == CostModel.COMMISSION && request.commissionPercent() == null) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "commissionPercent");
        }
        if (request.eventEndsAt() != null && request.eventStartsAt() != null && !request.eventEndsAt().isAfter(request.eventStartsAt())) {
            throw ApiException.field(ErrorCode.VALIDATION_FAILED, "eventEndsAt");
        }
    }

    private static AdminCatalogDtos.CategoryView view(Category category) {
        return new AdminCatalogDtos.CategoryView(category.getId(), category.slug(), category.name(), category.iconKey(), category.sortOrder(),
                category.active());
    }

    private static AdminCatalogDtos.PartnerView view(Partner partner) {
        return new AdminCatalogDtos.PartnerView(partner.getId(), partner.slug(), partner.name(), partner.description(), partner.city(),
                partner.address(), partner.websiteUrl(), partner.logoPath(), partner.demo(), partner.status().name());
    }

    private static AdminCatalogDtos.ProductView view(PartnerProduct product, ProductInventory stock) {
        return new AdminCatalogDtos.ProductView(product.getId(), product.partner().getId(), product.partner().name(), product.category().getId(),
                product.productType().name(), product.title(), product.description(), product.terms(), product.redemptionInstructions(),
                product.eligibilityNote(), product.retailPrice(), product.costModel().name(), product.unitNetCost(), product.commissionPercent(),
                product.validFrom(), product.validUntil(), product.eventStartsAt(), product.eventEndsAt(), product.venueName(),
                product.venueAddress(), product.city(), product.redemptionMethod().name(), product.refundable(), product.refundDeadlineHours(),
                product.restockOnRefund(), product.status().name(), stock == null ? 0 : stock.totalQuantity(),
                stock == null ? 0 : stock.reservedQuantity(), stock == null ? 0 : stock.soldQuantity(), stock == null ? 0 : stock.available());
    }

    private static AdminCatalogDtos.OfferView view(Offer offer, PriceBreakdown breakdown, int available) {
        List<AdminCatalogDtos.ComponentView> components = offer.components().stream()
                .map(component -> new AdminCatalogDtos.ComponentView(component.product().getId(), component.product().title().en(),
                        component.product().partner().name(), component.quantity(), component.negotiatedUnitCost()))
                .toList();
        return new AdminCatalogDtos.OfferView(offer.getId(), offer.slug(), offer.offerType().name(), offer.status().name(),
                offer.visibility().name(), offer.primaryCategory().getId(), offer.title(), offer.subtitle(), offer.description(),
                offer.imagePath(), offer.sellingPrice(), offer.saleStartsAt(), offer.saleEndsAt(), offer.maxPerOrder(), offer.businessPriority(),
                offer.featured(), offer.tags(), components, breakdown, available, offer.publishedAt());
    }

    private static AdminCatalogDtos.CampaignView view(Campaign campaign) {
        return new AdminCatalogDtos.CampaignView(campaign.getId(), campaign.slug(), campaign.name(), campaign.bannerText(),
                campaign.audience().name(), campaign.startsAt(), campaign.endsAt(), campaign.bonusPointsPerUnit(), campaign.featured(),
                campaign.status().name(), campaign.offers().stream().map(Offer::getId).toList());
    }
}
