package az.buta.order.dto;

import az.buta.common.money.MoneyDto;

import java.time.Instant;
import java.util.UUID;

public record OrderSummaryDto(UUID id, String orderNumber, String status, MoneyDto total, int itemCount, String firstItemTitle,
                              Instant createdAt) {
}
