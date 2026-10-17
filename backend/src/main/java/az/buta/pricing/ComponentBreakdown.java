package az.buta.pricing;

import az.buta.catalog.domain.CostModel;

import java.math.BigDecimal;
import java.util.UUID;

public record ComponentBreakdown(UUID productId, UUID partnerId, int quantity, CostModel costModel, BigDecimal retailValue,
                                 BigDecimal allocatedPrice, BigDecimal partnerCost) {
}
