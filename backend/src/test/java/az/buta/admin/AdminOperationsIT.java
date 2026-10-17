package az.buta.admin;

import az.buta.audit.AuditLogRepository;
import az.buta.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminOperationsIT extends IntegrationTest {

    private static final String PREMIERE = "30000000-0000-0000-0000-000000000004";
    private static final String GALLERY = "30000000-0000-0000-0000-000000000011";

    @Autowired
    private AuditLogRepository auditLogs;

    @Test
    void marginPreviewReproducesTheWorkedExample() throws Exception {
        String body = "{\"sellingPrice\":\"28.00\",\"components\":[{\"productId\":\"" + PREMIERE + "\",\"quantity\":1},"
                + "{\"productId\":\"" + GALLERY + "\",\"quantity\":1}]}";
        postJson("/api/v1/admin/offers/margin-preview", body, sessionFor(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceRetail", is(35.00)))
                .andExpect(jsonPath("$.savings", is(7.00)))
                .andExpect(jsonPath("$.partnerCostTotal", is(25.00)))
                .andExpect(jsonPath("$.paymentFee", is(0.90)))
                .andExpect(jsonPath("$.expectedLoyaltyCost", is(0.25)))
                .andExpect(jsonPath("$.contributionMargin", is(1.55)))
                .andExpect(jsonPath("$.marginPercent", is(5.54)))
                .andExpect(jsonPath("$.sellable", is(true)));
    }

    @Test
    void unprofitableBundleCannotBePublished() throws Exception {
        Cookie admin = sessionFor(ADMIN);
        String body = """
                {"offerType":"BUNDLE","primaryCategoryId":"10000000-0000-0000-0000-000000000001",
                 "title":{"az":"Zərərli paket","en":"Loss-making bundle"},"subtitle":{"az":"Test","en":"Test"},
                 "description":{"az":"Test","en":"Test"},"sellingPrice":"22.00","visibility":"PUBLIC",
                 "saleStartsAt":"2026-01-01T00:00:00Z","saleEndsAt":"2099-01-01T00:00:00Z","maxPerOrder":2,"businessPriority":0,
                 "featured":false,"tags":[],"components":[{"productId":"%s","quantity":1},{"productId":"%s","quantity":1}]}
                """.formatted(PREMIERE, GALLERY);
        JsonNode draft = json(postJson("/api/v1/admin/offers", body, admin).andExpect(status().isCreated()));
        assertThat(draft.path("status").asText()).isEqualTo("DRAFT");
        assertThat(draft.path("breakdown").path("sellable").asBoolean()).isFalse();
        postJson("/api/v1/admin/offers/" + draft.path("id").asText() + "/publish", "{}", admin)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("PUBLISH_RULE_FAILED")))
                .andExpect(jsonPath("$.failures", hasItem("MIN_MARGIN_AMOUNT")));
        assertThat(auditLogs.findByActionOrderByCreatedAtDesc("OFFER_CREATED")).isNotEmpty();
    }

    @Test
    void recommendationWeightsMustSumToHundred() throws Exception {
        Cookie admin = sessionFor(ADMIN);
        JsonNode current = json(perform(get("/api/v1/admin/settings/recommendation"), admin).andExpect(status().isOk()));
        ((com.fasterxml.jackson.databind.node.ObjectNode) current.path("weights")).put("interest", 50);
        perform(put("/api/v1/admin/settings/recommendation").contentType(MediaType.APPLICATION_JSON).content(current.toString()), admin)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code", is("WEIGHTS_INVALID")));
    }

    @Test
    void pricingImpactPreviewListsOffersThatWouldFail() throws Exception {
        String strict = "{\"paymentFeePercent\":2.50,\"paymentFeeFixed\":0.20,\"otherVariableCostPercent\":0,\"otherVariableCostFixed\":0.30,"
                + "\"minMarginAmount\":3.00,\"minMarginPercent\":5.00,\"lowStockThreshold\":5,\"reservationTtlMinutes\":15}";
        JsonNode preview = json(postJson("/api/v1/admin/settings/pricing/impact-preview", strict, sessionFor(ADMIN)).andExpect(status().isOk()));
        assertThat(preview.path("failingOffers").toString()).contains("cinema-and-exhibition-evening");
    }

    @Test
    void adminAssignsPlusAndActionIsAudited() throws Exception {
        Cookie customer = newCustomer();
        String userId = currentUserId(customer).toString();
        postJson("/api/v1/admin/users/" + userId + "/membership", "{\"reason\":\"Pilot member\"}", sessionFor(ADMIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plan", is("PLUS")));
        perform(get("/api/v1/me/loyalty"), customer).andExpect(jsonPath("$.plan", is("PLUS")));
        assertThat(auditLogs.findByActionOrderByCreatedAtDesc("PLUS_ASSIGNED")).isNotEmpty();
        perform(get("/api/v1/admin/audit-logs").param("action", "PLUS_ASSIGNED"), sessionFor(ADMIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].action", is("PLUS_ASSIGNED")));
    }

    @Test
    void adminRefundsAndReportsPartnerPerformance() throws Exception {
        Cookie customer = newCustomer();
        JsonNode order = buy(customer, "pottery-workshop", 1);
        postJson("/api/v1/admin/orders/" + order.path("id").asText() + "/refunds", "{\"reason\":\"\"}", sessionFor(ADMIN))
                .andExpect(status().isBadRequest());
        postJson("/api/v1/admin/orders/" + order.path("id").asText() + "/refunds", "{\"reason\":\"Partner cancelled the session\"}", sessionFor(ADMIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("REFUNDED")));
        perform(get("/api/v1/admin/partners/20000000-0000-0000-0000-000000000006/performance"), sessionFor(ADMIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.partnerName", is("Maker Loft Workshops")));
        perform(get("/api/v1/admin/recommendations/metrics"), sessionFor(ADMIN)).andExpect(status().isOk());
    }

    @Test
    void imageUrlsAreRestrictedToSafeSchemes() throws Exception {
        String body = """
                {"name":"Evil Partner","description":{"az":"x","en":"x"},"city":"Baku","websiteUrl":"javascript:alert(1)",
                 "demo":true,"status":"ACTIVE"}
                """;
        postJson("/api/v1/admin/partners", body, sessionFor(ADMIN)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("websiteUrl")));
    }
}
