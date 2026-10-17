package az.buta.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddCartItemRequest(@NotNull UUID offerId, @Min(1) @Max(10) int quantity, @Size(max = 64) String recommendationRequestId) {
}
