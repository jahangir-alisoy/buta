package az.buta.order.dto;

import az.buta.common.money.MoneyDto;

import java.util.List;
import java.util.UUID;

public record QuoteDto(List<Line> lines, MoneyDto subtotal, long pointsToRedeem, MoneyDto pointsDiscount, MoneyDto total,
                       Long pointsToEarn, PointsOptions points, String paymentMode) {

    public record Line(UUID offerId, String title, int quantity, MoneyDto unitPrice, MoneyDto lineTotal) {
    }

    public record PointsOptions(boolean allowed, long available, long maxRedeemable, int step, int minimum, String pointValue) {
    }
}
