package az.buta.catalog.dto;

import az.buta.common.money.MoneyDto;

import java.time.Instant;
import java.util.UUID;

public record ComponentDto(UUID productId, String title, String description, PartnerRefDto partner, CategoryRefDto category,
                           String productType, int quantity, MoneyDto retailPrice, Instant validFrom, Instant validUntil,
                           Instant eventStartsAt, Instant eventEndsAt, String venueName, String venueAddress, String city,
                           String redemptionMethod, String redemptionInstructions, String terms, String eligibilityNote,
                           boolean refundable, int refundDeadlineHours) {
}
