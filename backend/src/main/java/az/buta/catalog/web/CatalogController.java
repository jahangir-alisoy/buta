package az.buta.catalog.web;

import az.buta.catalog.CatalogQueryService;
import az.buta.catalog.ViewerResolver;
import az.buta.catalog.domain.OfferType;
import az.buta.catalog.domain.ProductType;
import az.buta.catalog.dto.CategoryDto;
import az.buta.catalog.dto.HomeDto;
import az.buta.catalog.dto.OfferCardDto;
import az.buta.catalog.dto.OfferDetailDto;
import az.buta.catalog.dto.OfferSearchCriteria;
import az.buta.catalog.dto.OfferSort;
import az.buta.catalog.dto.PartnerDetailDto;
import az.buta.catalog.dto.PartnerSummaryDto;
import az.buta.common.web.PageResponse;
import az.buta.security.AuthenticatedUser;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogQueryService catalog;
    private final ViewerResolver viewers;

    public CatalogController(CatalogQueryService catalog, ViewerResolver viewers) {
        this.catalog = catalog;
        this.viewers = viewers;
    }

    @GetMapping("/home")
    public HomeDto home(@AuthenticationPrincipal AuthenticatedUser user) {
        return catalog.home(viewers.resolve(user));
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return catalog.categories(LocaleContextHolder.getLocale());
    }

    @GetMapping("/partners")
    public List<PartnerSummaryDto> partners() {
        return catalog.partners(LocaleContextHolder.getLocale());
    }

    @GetMapping("/partners/{slug}")
    public PartnerDetailDto partner(@PathVariable @Size(max = 80) String slug, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalog.partner(slug, viewers.resolve(user));
    }

    @GetMapping("/offers")
    public PageResponse<OfferCardDto> offers(@AuthenticationPrincipal AuthenticatedUser user,
                                             @RequestParam(required = false) @Size(max = 100) String q,
                                             @RequestParam(required = false) @Size(max = 60) String category,
                                             @RequestParam(required = false) OfferType offerType,
                                             @RequestParam(required = false) ProductType productType,
                                             @RequestParam(required = false) @Size(max = 80) String partner,
                                             @RequestParam(required = false) @Size(max = 60) String city,
                                             @RequestParam(required = false) @DecimalMin("0") BigDecimal minPrice,
                                             @RequestParam(required = false) @DecimalMin("0") BigDecimal maxPrice,
                                             @RequestParam(required = false) Instant eventFrom,
                                             @RequestParam(required = false) Instant eventTo,
                                             @RequestParam(defaultValue = "false") boolean plusOnly,
                                             @RequestParam(defaultValue = "false") boolean includeSoldOut,
                                             @RequestParam(defaultValue = "RECOMMENDED") OfferSort sort,
                                             @RequestParam(defaultValue = "0") @Min(0) @Max(1000) int page,
                                             @RequestParam(defaultValue = "24") @Min(1) @Max(48) int size) {
        OfferSearchCriteria criteria = new OfferSearchCriteria(q, category, offerType, productType, partner, city, minPrice, maxPrice,
                eventFrom, eventTo, plusOnly, includeSoldOut, sort, page, size);
        return catalog.search(criteria, viewers.resolve(user));
    }

    @GetMapping("/offers/{slug}")
    public OfferDetailDto offer(@PathVariable @Size(max = 100) String slug, @AuthenticationPrincipal AuthenticatedUser user) {
        return catalog.detail(slug, viewers.resolve(user));
    }
}
