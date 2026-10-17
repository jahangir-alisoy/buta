package az.buta.catalog.admin;

import az.buta.catalog.domain.OfferStatus;
import az.buta.pricing.PriceBreakdown;
import az.buta.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {

    private final AdminCatalogService catalog;
    private final PartnerPerformanceService performance;

    public AdminCatalogController(AdminCatalogService catalog, PartnerPerformanceService performance) {
        this.catalog = catalog;
        this.performance = performance;
    }

    @GetMapping("/categories")
    public List<AdminCatalogDtos.CategoryView> categories() {
        return catalog.categories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCatalogDtos.CategoryView createCategory(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody CategoryRequest request) {
        return catalog.createCategory(admin.userId(), request);
    }

    @PutMapping("/categories/{id}")
    public AdminCatalogDtos.CategoryView updateCategory(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                                        @Valid @RequestBody CategoryRequest request) {
        return catalog.updateCategory(admin.userId(), id, request);
    }

    @GetMapping("/partners")
    public List<AdminCatalogDtos.PartnerView> partners() {
        return catalog.partners();
    }

    @PostMapping("/partners")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCatalogDtos.PartnerView createPartner(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody PartnerRequest request) {
        return catalog.createPartner(admin.userId(), request);
    }

    @PutMapping("/partners/{id}")
    public AdminCatalogDtos.PartnerView updatePartner(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                                      @Valid @RequestBody PartnerRequest request) {
        return catalog.updatePartner(admin.userId(), id, request);
    }

    @GetMapping("/partners/{id}/performance")
    public AdminCatalogDtos.PartnerPerformance partnerPerformance(@PathVariable UUID id,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(Duration.ofDays(30)) : from;
        return performance.performance(id, start, end);
    }

    @GetMapping("/products")
    public List<AdminCatalogDtos.ProductView> products() {
        return catalog.products();
    }

    @GetMapping("/products/{id}")
    public AdminCatalogDtos.ProductView product(@PathVariable UUID id) {
        return catalog.product(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCatalogDtos.ProductView createProduct(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody ProductRequest request) {
        return catalog.createProduct(admin.userId(), request);
    }

    @PutMapping("/products/{id}")
    public AdminCatalogDtos.ProductView updateProduct(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                                      @Valid @RequestBody ProductRequest request) {
        return catalog.updateProduct(admin.userId(), id, request);
    }

    @PostMapping("/products/{id}/inventory-adjustments")
    public AdminCatalogDtos.ProductView adjustInventory(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                                        @Valid @RequestBody InventoryAdjustmentRequest request) {
        return catalog.adjustInventory(admin.userId(), id, request);
    }

    @GetMapping("/offers")
    public List<AdminCatalogDtos.OfferView> offers() {
        return catalog.offers();
    }

    @GetMapping("/offers/{id}")
    public AdminCatalogDtos.OfferView offer(@PathVariable UUID id) {
        return catalog.offer(id);
    }

    @PostMapping("/offers")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCatalogDtos.OfferView createOffer(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody OfferRequest request) {
        return catalog.createOffer(admin.userId(), request);
    }

    @PutMapping("/offers/{id}")
    public AdminCatalogDtos.OfferView updateOffer(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                                  @Valid @RequestBody OfferRequest request) {
        return catalog.updateOffer(admin.userId(), id, request);
    }

    @PostMapping("/offers/margin-preview")
    public PriceBreakdown marginPreview(@Valid @RequestBody MarginPreviewRequest request) {
        return catalog.marginPreview(request);
    }

    @PostMapping("/offers/{id}/publish")
    public AdminCatalogDtos.OfferView publish(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        return catalog.changeStatus(admin.userId(), id, OfferStatus.PUBLISHED);
    }

    @PostMapping("/offers/{id}/pause")
    public AdminCatalogDtos.OfferView pause(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        return catalog.changeStatus(admin.userId(), id, OfferStatus.PAUSED);
    }

    @PostMapping("/offers/{id}/archive")
    public AdminCatalogDtos.OfferView archive(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        return catalog.changeStatus(admin.userId(), id, OfferStatus.ARCHIVED);
    }

    @GetMapping("/campaigns")
    public List<AdminCatalogDtos.CampaignView> campaigns() {
        return catalog.campaigns();
    }

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCatalogDtos.CampaignView createCampaign(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody CampaignRequest request) {
        return catalog.createCampaign(admin.userId(), request);
    }

    @PutMapping("/campaigns/{id}")
    public AdminCatalogDtos.CampaignView updateCampaign(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
                                                        @Valid @RequestBody CampaignRequest request) {
        return catalog.updateCampaign(admin.userId(), id, request);
    }
}
