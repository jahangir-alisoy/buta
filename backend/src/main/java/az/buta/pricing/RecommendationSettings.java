package az.buta.pricing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RecommendationSettings(
        @NotNull @Valid Weights weights,
        @NotNull @Valid SignalWeights signalWeights,
        @Min(0) @Max(100) int minRelevance,
        @Min(0) @Max(50) int explorationPercent,
        @Min(1) int decayHalfLifeDays,
        @Min(1) int eventRetentionDays,
        @NotNull @Valid FrequencyCap frequencyCap,
        @NotNull @Valid Diversity diversity,
        @NotEmpty List<@Valid Variant> variants) {

    public record Weights(@Min(0) @Max(100) int interest, @Min(0) @Max(100) int budget, @Min(0) @Max(100) int value,
                          @Min(0) @Max(100) int loyalty, @Min(0) @Max(100) int discovery, @Min(0) @Max(100) int business) {

        public int total() {
            return interest + budget + value + loyalty + discovery + business;
        }
    }

    public record SignalWeights(double purchase, double explicitPreference, double favorite, double cartAdd,
                                double detailView, double search) {
    }

    public record FrequencyCap(@Min(1) int maxServes, @Min(1) int windowDays) {
    }

    public record Diversity(@Min(1) int maxPerPartnerTop10, @Min(1) int maxPerCategoryTop10) {
    }

    public record Variant(@NotBlank String name, @Min(0) @Max(100) int allocationPercent, @Valid Weights weights) {
    }
}
