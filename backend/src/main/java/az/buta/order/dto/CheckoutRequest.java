package az.buta.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CheckoutRequest(@Min(0) @Max(10_000_000) long pointsToRedeem) {
}
