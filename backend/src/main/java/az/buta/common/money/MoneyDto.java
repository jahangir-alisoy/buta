package az.buta.common.money;

import java.math.BigDecimal;

public record MoneyDto(String amount, String currency) {

    public static MoneyDto of(BigDecimal value) {
        return new MoneyDto(Amounts.scale(value).toPlainString(), Amounts.CURRENCY);
    }
}
