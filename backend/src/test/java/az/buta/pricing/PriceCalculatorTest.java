package az.buta.pricing;

import az.buta.catalog.domain.CostModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PriceCalculatorTest {

    private static final PricingSettings PRICING = new PricingSettings(new BigDecimal("2.50"), new BigDecimal("0.20"), BigDecimal.ZERO,
            new BigDecimal("0.30"), new BigDecimal("1.00"), new BigDecimal("5.00"), 5, 15);
    private static final LoyaltySettings LOYALTY = new LoyaltySettings(1, new BigDecimal("0.01"), new BigDecimal("70.00"), 100, 100,
            new BigDecimal("20.00"), AwardTrigger.FULFILLED, 0, new LoyaltySettings.Referral(new BigDecimal("10.00"), 5, 30));

    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void workedBundleExampleProducesExpectedMargin() {
        PriceBreakdown breakdown = calculate("28.00", 0, wholesale("20.00", "16.00", 1), wholesale("15.00", "9.00", 1));

        assertThat(breakdown.referenceRetail()).isEqualByComparingTo("35.00");
        assertThat(breakdown.savings()).isEqualByComparingTo("7.00");
        assertThat(breakdown.savingsPercent()).isEqualByComparingTo("20.00");
        assertThat(breakdown.partnerCostTotal()).isEqualByComparingTo("25.00");
        assertThat(breakdown.paymentFee()).isEqualByComparingTo("0.90");
        assertThat(breakdown.maxPointsEarned()).isEqualTo(35);
        assertThat(breakdown.expectedLoyaltyCost()).isEqualByComparingTo("0.25");
        assertThat(breakdown.otherVariableCost()).isEqualByComparingTo("0.30");
        assertThat(breakdown.contributionMargin()).isEqualByComparingTo("1.55");
        assertThat(breakdown.marginPercent()).isEqualByComparingTo("5.54");
        assertThat(breakdown.sellable()).isTrue();
    }

    @Test
    void higherPartnerCostMakesBundleUnsellable() {
        PriceBreakdown breakdown = calculate("28.00", 0, wholesale("20.00", "18.00", 1), wholesale("15.00", "9.00", 1));

        assertThat(breakdown.contributionMargin()).isEqualByComparingTo("-0.45");
        assertThat(breakdown.sellable()).isFalse();
        assertThat(breakdown.rules()).filteredOn(rule -> !rule.passed()).extracting(MarginRule::code)
                .containsExactly("MIN_MARGIN_AMOUNT", "MIN_MARGIN_PERCENT");
    }

    @Test
    void commissionModelUsesAllocatedPrice() {
        PriceBreakdown breakdown = calculate("28.00", 0, commission("20.00", "10.00", 1), wholesale("15.00", "9.00", 1));

        assertThat(breakdown.components().get(0).allocatedPrice()).isEqualByComparingTo("16.00");
        assertThat(breakdown.components().get(0).partnerCost()).isEqualByComparingTo("14.40");
        assertThat(breakdown.components().get(1).allocatedPrice()).isEqualByComparingTo("12.00");
    }

    @Test
    void allocationRemainderGoesToLastComponentSoSharesSumExactly() {
        PriceBreakdown breakdown = calculate("10.00", 0, wholesale("3.00", "1.00", 1), wholesale("3.00", "1.00", 1), wholesale("3.00", "1.00", 1));

        BigDecimal total = breakdown.components().stream().map(ComponentBreakdown::allocatedPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("10.00");
        assertThat(breakdown.components().get(2).allocatedPrice()).isEqualByComparingTo("3.34");
    }

    @Test
    void negotiatedBundleCostOverridesProductCost() {
        PricingComponent negotiated = new PricingComponent(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("30.00"), 1, CostModel.WHOLESALE,
                new BigDecimal("21.00"), null, new BigDecimal("19.50"));
        PriceBreakdown breakdown = calculate("30.00", 0, negotiated, wholesale("10.00", "6.00", 1));

        assertThat(breakdown.partnerCostTotal()).isEqualByComparingTo("25.50");
    }

    @Test
    void campaignBonusPointsIncreaseExpectedLoyaltyCost() {
        PriceBreakdown withoutBonus = calculate("12.00", 0, commission("14.00", "22.00", 1));
        PriceBreakdown withBonus = calculate("12.00", 10, commission("14.00", "22.00", 1));

        assertThat(withBonus.expectedLoyaltyCost()).isGreaterThan(withoutBonus.expectedLoyaltyCost());
        assertThat(withBonus.maxPointsEarned()).isEqualTo(withoutBonus.maxPointsEarned() + 10);
    }

    @Test
    void sellingAboveReferenceRetailIsRejected() {
        PriceBreakdown breakdown = calculate("40.00", 0, wholesale("35.00", "10.00", 1));

        assertThat(breakdown.savings()).isNegative();
        assertThat(breakdown.sellable()).isFalse();
    }

    @Test
    void pointsAreAlwaysFloored() {
        assertThat(PriceCalculator.maxPoints(new BigDecimal("38.00"), 1, new BigDecimal("1.10"))).isEqualTo(41);
        assertThat(PriceCalculator.maxPoints(new BigDecimal("8.50"), 1, BigDecimal.ONE)).isEqualTo(8);
    }

    private PriceBreakdown calculate(String price, int bonus, PricingComponent... components) {
        return calculator.calculate(new PricingInput(new BigDecimal(price), List.of(components), PRICING, LOYALTY, new BigDecimal("1.25"), bonus));
    }

    private static PricingComponent wholesale(String retail, String cost, int quantity) {
        return new PricingComponent(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(retail), quantity, CostModel.WHOLESALE,
                new BigDecimal(cost), null, null);
    }

    private static PricingComponent commission(String retail, String percent, int quantity) {
        return new PricingComponent(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(retail), quantity, CostModel.COMMISSION, null,
                new BigDecimal(percent), null);
    }
}
