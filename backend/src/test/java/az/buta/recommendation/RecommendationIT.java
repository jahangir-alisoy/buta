package az.buta.recommendation;

import az.buta.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecommendationIT extends IntegrationTest {

    @Test
    void cinemaFanReceivesPersonalizedCinemaRecommendationsWithReasons() throws Exception {
        JsonNode result = json(perform(get("/api/v1/recommendations").param("placement", "FOR_YOU").param("limit", "10"), sessionFor(KAMRAN))
                .andExpect(status().isOk()));
        assertThat(result.path("personalized").asBoolean()).isTrue();
        assertThat(result.path("items").size()).isGreaterThan(3);
        JsonNode first = result.path("items").get(0);
        assertThat(first.path("offer").path("category").path("slug").asText()).isEqualTo("cinema");
        assertThat(first.path("reasonCode").asText()).isIn("REASON_CATEGORY_INTEREST", "REASON_NEXT_BENEFIT", "REASON_TASK_PROGRESS");
        List<String> reasons = new ArrayList<>();
        result.path("items").forEach(item -> reasons.add(item.path("reasonCode").asText()));
        assertThat(reasons).contains("REASON_NEXT_BENEFIT");
    }

    @Test
    void personalizedListsRespectDiversityCaps() throws Exception {
        JsonNode result = json(perform(get("/api/v1/recommendations").param("placement", "FOR_YOU").param("limit", "10"), sessionFor(KAMRAN)));
        Map<String, Integer> perPartner = new HashMap<>();
        result.path("items").forEach(item -> {
            if (!item.path("exploration").asBoolean()) {
                item.path("offer").path("partners").forEach(partner -> perPartner.merge(partner.path("slug").asText(), 1, Integer::sum));
            }
        });
        assertThat(perPartner.values()).allMatch(count -> count <= 2);
    }

    @Test
    void theatreLoverSeesCulturalOffers() throws Exception {
        JsonNode result = json(perform(get("/api/v1/recommendations").param("placement", "FOR_YOU").param("limit", "6"), sessionFor(LEYLA)));
        assertThat(result.path("personalized").asBoolean()).isTrue();
        List<String> categories = new ArrayList<>();
        result.path("items").forEach(item -> categories.add(item.path("offer").path("category").path("slug").asText()));
        assertThat(categories.subList(0, 2)).containsAnyOf("theatre", "exhibitions", "museums");
    }

    @Test
    void anonymousAndFreeUsersGetNonPersonalizedEligibleOffersOnly() throws Exception {
        JsonNode anonymous = json(mvc.perform(get("/api/v1/recommendations")).andExpect(status().isOk()));
        assertThat(anonymous.path("personalized").asBoolean()).isFalse();
        anonymous.path("items").forEach(item -> {
            assertThat(item.path("offer").path("purchasable").asBoolean()).isTrue();
            assertThat(item.path("offer").path("plusOnly").asBoolean()).isFalse();
            assertThat(item.path("offer").path("availability").asText()).isNotEqualTo("SOLD_OUT");
        });
        JsonNode free = json(perform(get("/api/v1/recommendations"), sessionFor(AYSEL)));
        assertThat(free.path("personalized").asBoolean()).isFalse();
    }

    @Test
    void plusMemberWithoutConsentOrHistoryGetsFallback() throws Exception {
        Cookie session = newCustomer();
        JsonNode result = json(perform(get("/api/v1/recommendations").param("placement", "FOR_YOU"), session));
        assertThat(result.path("personalized").asBoolean()).isFalse();
        assertThat(result.path("items").size()).isGreaterThan(0);
    }

    @Test
    void similarOffersExcludeTheCurrentOffer() throws Exception {
        String offerId = offerId("imax-premiere").toString();
        JsonNode result = json(mvc.perform(get("/api/v1/recommendations").param("placement", "OFFER_DETAIL").param("offerId", offerId)));
        result.path("items").forEach(item -> assertThat(item.path("offer").path("id").asText()).isNotEqualTo(offerId));
    }

    @Test
    void interactionsAreAcceptedAndPersonalizationCanBeReset() throws Exception {
        Cookie session = sessionFor(LEYLA);
        postJson("/api/v1/interactions", "{\"events\":[{\"type\":\"DETAIL_VIEW\",\"offerId\":\"" + offerId("the-glass-orchard") + "\"}]}", session)
                .andExpect(status().isAccepted());
        postJson("/api/v1/interactions", "{\"events\":[{\"type\":\"PURCHASE\"}]}", session).andExpect(status().isBadRequest());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/interactions")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json").content("{\"events\":[{\"type\":\"DETAIL_VIEW\"}]}")).andExpect(status().isAccepted());
        perform(delete("/api/v1/me/personalization-data"), sessionFor(AYSEL)).andExpect(status().isNoContent());
    }
}
