package az.buta.pricing;

import az.buta.catalog.domain.CostModel;
import az.buta.common.money.Amounts;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class PriceCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public PriceBreakdown calculate(PricingInput input) {
        BigDecimal sellingPrice = Amounts.scale(input.sellingPrice());
        List<BigDecimal> retailValues = input.components().stream()
                .map(component -> Amounts.scale(component.retailPrice().multiply(BigDecimal.valueOf(component.quantity()))))
                .toList();
        BigDecimal referenceRetail = Amounts.scale(retailValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        List<BigDecimal> allocations = allocate(sellingPrice, retailValues, referenceRetail);

        List<ComponentBreakdown> components = new ArrayList<>();
        BigDecimal partnerCostTotal = BigDecimal.ZERO;
        for (int index = 0; index < input.components().size(); index++) {
            PricingComponent component = input.components().get(index);
            BigDecimal partnerCost = partnerCost(component, allocations.get(index));
            partnerCostTotal = partnerCostTotal.add(partnerCost);
            components.add(new ComponentBreakdown(component.productId(), component.partnerId(), component.quantity(),
                    component.costModel(), retailValues.get(index), allocations.get(index), partnerCost));
        }
        partnerCostTotal = Amounts.scale(partnerCostTotal);

        PricingSettings pricing = input.pricing();
        LoyaltySettings loyalty = input.loyalty();
        BigDecimal paymentFee = Amounts.scale(Amounts.percentOf(sellingPrice, pricing.paymentFeePercent()).add(pricing.paymentFeeFixed()));
        long maxPoints = maxPoints(sellingPrice, loyalty.pointsPerAzn(), input.highestTierMultiplier()) + input.maxCampaignBonusPoints();
        BigDecimal expectedLoyaltyCost = Amounts.scale(BigDecimal.valueOf(maxPoints)
                .multiply(loyalty.pointValueAzn())
                .multiply(loyalty.expectedRedemptionPercent())
                .divide(HUNDRED, 10, RoundingMode.HALF_UP));
        BigDecimal otherVariableCost = Amounts.scale(Amounts.percentOf(sellingPrice, pricing.otherVariableCostPercent())
                .add(pricing.otherVariableCostFixed()));
        BigDecimal margin = Amounts.scale(sellingPrice.subtract(partnerCostTotal).subtract(paymentFee)
                .subtract(expectedLoyaltyCost).subtract(otherVariableCost));
        BigDecimal marginPercent = Amounts.ratioPercent(margin, sellingPrice);
        BigDecimal savings = Amounts.scale(referenceRetail.subtract(sellingPrice));
        BigDecimal savingsPercent = Amounts.ratioPercent(savings.max(BigDecimal.ZERO), referenceRetail);

        List<MarginRule> rules = List.of(
                new MarginRule("NON_NEGATIVE_SAVINGS", null, savings.signum() >= 0),
                new MarginRule("MIN_MARGIN_AMOUNT", Amounts.scale(pricing.minMarginAmount()), margin.compareTo(pricing.minMarginAmount()) >= 0),
                new MarginRule("MIN_MARGIN_PERCENT", Amounts.scale(pricing.minMarginPercent()), marginPercent.compareTo(pricing.minMarginPercent()) >= 0));
        boolean sellable = !components.isEmpty() && rules.stream().allMatch(MarginRule::passed);
        return new PriceBreakdown(sellingPrice, referenceRetail, savings, savingsPercent, components, partnerCostTotal, paymentFee,
                maxPoints, expectedLoyaltyCost, otherVariableCost, margin, marginPercent, rules, sellable);
    }

    public static long maxPoints(BigDecimal amount, int pointsPerAzn, BigDecimal multiplier) {
        return amount.multiply(BigDecimal.valueOf(pointsPerAzn)).multiply(multiplier).setScale(0, RoundingMode.FLOOR).longValue();
    }

    private static List<BigDecimal> allocate(BigDecimal sellingPrice, List<BigDecimal> retailValues, BigDecimal referenceRetail) {
        List<BigDecimal> allocations = new ArrayList<>();
        if (retailValues.isEmpty()) {
            return allocations;
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < retailValues.size() - 1; index++) {
            BigDecimal share = referenceRetail.signum() == 0
                    ? BigDecimal.ZERO
                    : Amounts.scale(sellingPrice.multiply(retailValues.get(index)).divide(referenceRetail, 10, RoundingMode.HALF_UP));
            allocations.add(share);
            allocated = allocated.add(share);
        }
        allocations.add(Amounts.scale(sellingPrice.subtract(allocated)));
        return allocations;
    }

    private static BigDecimal partnerCost(PricingComponent component, BigDecimal allocatedPrice) {
        BigDecimal quantity = BigDecimal.valueOf(component.quantity());
        if (component.negotiatedUnitCost() != null) {
            return Amounts.scale(component.negotiatedUnitCost().multiply(quantity));
        }
        if (component.costModel() == CostModel.WHOLESALE) {
            return Amounts.scale(component.unitNetCost().multiply(quantity));
        }
        BigDecimal keepShare = HUNDRED.subtract(component.commissionPercent()).divide(HUNDRED, 10, RoundingMode.HALF_UP);
        return Amounts.scale(allocatedPrice.multiply(keepShare));
    }
}
