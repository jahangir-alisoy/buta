package az.buta.pricing;

import java.math.BigDecimal;

public record MarginRule(String code, BigDecimal threshold, boolean passed) {
}
