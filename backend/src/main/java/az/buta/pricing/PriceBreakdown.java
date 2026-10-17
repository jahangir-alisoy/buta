package az.buta.pricing;

import java.math.BigDecimal;
import java.util.List;

public record PriceBreakdown(BigDecimal sellingPrice, BigDecimal referenceRetail, BigDecimal savings, BigDecimal savingsPercent,
                             List<ComponentBreakdown> components, BigDecimal partnerCostTotal, BigDecimal paymentFee,
                             long maxPointsEarned, BigDecimal expectedLoyaltyCost, BigDecimal otherVariableCost,
                             BigDecimal contributionMargin, BigDecimal marginPercent, List<MarginRule> rules, boolean sellable) {
}
