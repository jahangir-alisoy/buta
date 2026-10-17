package az.buta.membership;

import az.buta.common.money.MoneyDto;

import java.time.Instant;

public record PlusPlanDto(MoneyDto price, long periodDays, boolean forSale, boolean canPurchase, String plan, String source,
                          Instant endsAt) {
}
