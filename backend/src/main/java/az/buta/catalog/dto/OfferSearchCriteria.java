package az.buta.catalog.dto;

import az.buta.catalog.domain.OfferType;
import az.buta.catalog.domain.ProductType;

import java.math.BigDecimal;
import java.time.Instant;

public record OfferSearchCriteria(String q, String category, OfferType offerType, ProductType productType, String partner,
                                  String city, BigDecimal minPrice, BigDecimal maxPrice, Instant eventFrom, Instant eventTo,
                                  boolean plusOnly, boolean includeSoldOut, OfferSort sort, int page, int size) {
}
