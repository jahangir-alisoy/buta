package az.buta.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(@Min(1) @Max(10) int quantity) {
}
