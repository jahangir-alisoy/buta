package az.buta.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Amounts {

    public static final String CURRENCY = "AZN";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Amounts() {
    }

    public static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentOf(BigDecimal value, BigDecimal percent) {
        return scale(value.multiply(percent).divide(HUNDRED, 10, RoundingMode.HALF_UP));
    }

    public static BigDecimal ratioPercent(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return scale(BigDecimal.ZERO);
        }
        return part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() {
        return scale(BigDecimal.ZERO);
    }
}
