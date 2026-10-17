package az.buta.pricing;

import az.buta.catalog.domain.CostModel;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingComponent(UUID productId, UUID partnerId, BigDecimal retailPrice, int quantity, CostModel costModel,
                               BigDecimal unitNetCost, BigDecimal commissionPercent, BigDecimal negotiatedUnitCost) {
}
