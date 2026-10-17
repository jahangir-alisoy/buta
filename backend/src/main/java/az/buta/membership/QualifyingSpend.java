package az.buta.membership;

import java.math.BigDecimal;

public record QualifyingSpend(BigDecimal spent, BigDecimal threshold, BigDecimal remaining, int windowDays) {
}
