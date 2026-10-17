package az.buta.cart;

import az.buta.catalog.dto.OfferCardDto;
import az.buta.common.money.MoneyDto;

import java.util.List;
import java.util.UUID;

public record CartDto(List<Line> items, MoneyDto subtotal, int itemCount, boolean checkoutReady, Long pointsPreview) {

    public record Line(UUID id, OfferCardDto offer, int quantity, MoneyDto unitPrice, MoneyDto unitPriceAtAdd, boolean priceChanged,
                       MoneyDto lineTotal, String issue, int maxQuantity) {
    }
}
