package az.buta.order;

import az.buta.catalog.domain.ProductInventoryRepository;
import az.buta.loyalty.LedgerService;
import az.buta.loyalty.domain.LedgerEntryRepository;
import az.buta.loyalty.domain.LedgerEntryType;
import az.buta.loyalty.domain.LedgerSourceType;
import az.buta.membership.MembershipService;
import az.buta.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckoutIT extends IntegrationTest {

    private static final UUID PREMIERE_PRODUCT = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID GALLERY_PRODUCT = UUID.fromString("30000000-0000-0000-0000-000000000011");

    @Autowired
    private ProductInventoryRepository inventory;

    @Autowired
    private OrderLifecycleService lifecycle;

    @Autowired
    private MembershipService membership;

    @Autowired
    private LedgerService ledger;

    @Autowired
    private LedgerEntryRepository ledgerEntries;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void customerBuysBundleThroughSandboxPayment() throws Exception {
        Cookie session = newCustomer();
        int premiereBefore = available(PREMIERE_PRODUCT);
        addToCart(session, "cinema-and-exhibition-evening", 1);

        postJson("/api/v1/checkout/quote", "{\"pointsToRedeem\":0}", session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total.amount", is("28.00")))
                .andExpect(jsonPath("$.paymentMode", is("SANDBOX")))
                .andExpect(jsonPath("$.points.allowed", is(false)));

        JsonNode order = json(checkout(session, "key-" + UUID.randomUUID(), 0).andExpect(status().isCreated()));
        assertThat(order.path("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.path("total").path("amount").asText()).isEqualTo("28.00");
        assertThat(order.path("items").get(0).path("vouchers").size()).isZero();
        assertThat(available(PREMIERE_PRODUCT)).isEqualTo(premiereBefore - 1);
        perform(get("/api/v1/cart"), session).andExpect(jsonPath("$.items.length()", is(0)));

        JsonNode paid = json(confirm(session, order, "SUCCEEDED").andExpect(status().isOk()));
        assertThat(paid.path("status").asText()).isEqualTo("FULFILLED");
        assertThat(paid.path("items").get(0).path("vouchers").size()).isEqualTo(2);
        assertThat(paid.path("items").get(0).path("pointsEarned").asLong()).isZero();
        confirm(session, order, "SUCCEEDED").andExpect(status().isOk()).andExpect(jsonPath("$.status", is("FULFILLED")));
        confirm(session, order, "FAILED").andExpect(status().isConflict());
        assertThat(available(PREMIERE_PRODUCT)).isEqualTo(premiereBefore - 1);
    }

    @Test
    void checkoutIsIdempotentPerKey() throws Exception {
        Cookie session = newCustomer();
        addToCart(session, "movie-and-coffee", 1);
        String key = "key-" + UUID.randomUUID();
        String first = json(checkout(session, key, 0).andExpect(status().isCreated())).path("id").asText();
        String second = json(checkout(session, key, 0).andExpect(status().isCreated())).path("id").asText();
        assertThat(second).isEqualTo(first);
        addToCart(session, "coffee-and-pastry", 2);
        checkout(session, key, 0).andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("IDEMPOTENCY_CONFLICT")));
        mvc.perform(post("/api/v1/checkout").with(csrf()).cookie(session).contentType(MediaType.APPLICATION_JSON).content("{\"pointsToRedeem\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("IDEMPOTENCY_KEY_REQUIRED")));
    }

    @Test
    void failedPaymentReleasesInventoryAndRestoresCart() throws Exception {
        Cookie session = newCustomer();
        int before = available(GALLERY_PRODUCT);
        addToCart(session, "linen-evening-admission", 2);
        JsonNode order = json(checkout(session, "key-" + UUID.randomUUID(), 0));
        assertThat(available(GALLERY_PRODUCT)).isEqualTo(before - 2);
        confirm(session, order, "FAILED").andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.cancelReason", is("PAYMENT_FAILED")));
        assertThat(available(GALLERY_PRODUCT)).isEqualTo(before);
        perform(get("/api/v1/cart"), session).andExpect(jsonPath("$.items[0].quantity", is(2)));
    }

    @Test
    void pendingOrdersExpireAndReleaseReservations() throws Exception {
        Cookie session = newCustomer();
        int before = available(GALLERY_PRODUCT);
        addToCart(session, "linen-evening-admission", 1);
        JsonNode order = json(checkout(session, "key-" + UUID.randomUUID(), 0));
        clock.advance(Duration.ofMinutes(16));
        confirm(session, order, "SUCCEEDED").andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("ORDER_STATE_INVALID")));
        assertThat(lifecycle.expireDuePending(50)).isGreaterThanOrEqualTo(1);
        perform(get("/api/v1/orders/" + order.path("id").asText()), session).andExpect(jsonPath("$.status", is("EXPIRED")));
        assertThat(available(GALLERY_PRODUCT)).isEqualTo(before);
    }

    @Test
    void customerCancelsPendingOrder() throws Exception {
        Cookie session = newCustomer();
        addToCart(session, "coffee-and-pastry", 1);
        JsonNode order = json(checkout(session, "key-" + UUID.randomUUID(), 0));
        postJson("/api/v1/orders/" + order.path("id").asText() + "/cancel", "{}", session)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("CANCELLED")));
        postJson("/api/v1/orders/" + order.path("id").asText() + "/cancel", "{}", session).andExpect(status().isConflict());
    }

    @Test
    void refundVoidsVouchersAndRestocksInventory() throws Exception {
        Cookie session = newCustomer();
        int before = available(PREMIERE_PRODUCT);
        addToCart(session, "premiere-evening", 1);
        JsonNode order = json(checkout(session, "key-" + UUID.randomUUID(), 0));
        confirm(session, order, "SUCCEEDED");
        assertThat(available(PREMIERE_PRODUCT)).isEqualTo(before - 1);
        JsonNode refunded = json(postJson("/api/v1/orders/" + order.path("id").asText() + "/refund", "{}", session).andExpect(status().isOk()));
        assertThat(refunded.path("status").asText()).isEqualTo("REFUNDED");
        assertThat(refunded.path("payment").path("status").asText()).isEqualTo("REFUNDED");
        assertThat(refunded.path("items").get(0).path("vouchers").get(0).path("status").asText()).isEqualTo("VOIDED");
        assertThat(available(PREMIERE_PRODUCT)).isEqualTo(before);
        postJson("/api/v1/orders/" + order.path("id").asText() + "/refund", "{}", session).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void plusOnlyOffersRequirePlusAndQuantitiesAreBounded() throws Exception {
        Cookie session = newCustomer();
        postJson("/api/v1/cart/items", "{\"offerId\":\"" + offerId("late-night-gallery-preview") + "\",\"quantity\":1}", session)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code", is("PLUS_REQUIRED")));
        postJson("/api/v1/cart/items", "{\"offerId\":\"" + offerId("evening-2d-screening") + "\",\"quantity\":7}", session)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code", is("QUANTITY_LIMIT")));
        postJson("/api/v1/checkout/quote", "{\"pointsToRedeem\":100}", session).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("CART_EMPTY")));
    }

    @Test
    void plusMemberEarnsPointsOnFulfilmentRedeemsAndLosesThemOnRefund() throws Exception {
        Cookie session = newCustomer();
        UUID userId = currentUserId(session);
        membership.assignPlus(userId, userId, null, "test");
        transactions.executeWithoutResult(status -> ledger.post(new LedgerService.PostingRequest(userId, LedgerEntryType.ADJUSTMENT, 300,
                "TEST:" + userId, LedgerSourceType.ADMIN, null, null, "ledger.adjustment", Map.of(), null)));

        addToCart(session, "festival-day-pass", 1);
        postJson("/api/v1/checkout/quote", "{\"pointsToRedeem\":100}", session)
                .andExpect(jsonPath("$.points.allowed", is(true)))
                .andExpect(jsonPath("$.pointsDiscount.amount", is("1.00")))
                .andExpect(jsonPath("$.total.amount", is("38.00")));
        postJson("/api/v1/checkout/quote", "{\"pointsToRedeem\":150}", session).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("POINTS_REDEMPTION_LIMIT")));
        postJson("/api/v1/checkout/quote", "{\"pointsToRedeem\":900}", session).andExpect(status().isUnprocessableEntity());

        JsonNode order = json(checkout(session, "key-" + UUID.randomUUID(), 100));
        assertThat(availablePoints(session)).isEqualTo(200);
        assertThat(ledgerEntries.findByIdempotencyKey("EARN_PURCHASE:" + order.path("items").get(0).path("id").asText())).isEmpty();

        JsonNode paid = json(confirm(session, order, "SUCCEEDED"));
        long earned = paid.path("pointsEarned").asLong();
        assertThat(earned).isEqualTo(41L);
        assertThat(availablePoints(session)).isEqualTo(200 + earned);

        postJson("/api/v1/orders/" + order.path("id").asText() + "/refund", "{}", session).andExpect(status().isOk());
        assertThat(availablePoints(session)).isEqualTo(300);
        perform(get("/api/v1/me/loyalty"), session).andExpect(jsonPath("$.redeemedPoints", is(0)))
                .andExpect(jsonPath("$.lifetimeEarnedPoints", is(300)));
    }

    private long availablePoints(Cookie session) throws Exception {
        return json(perform(get("/api/v1/me/loyalty"), session)).path("availablePoints").asLong();
    }

    private void addToCart(Cookie session, String slug, int quantity) throws Exception {
        postJson("/api/v1/cart/items", "{\"offerId\":\"" + offerId(slug) + "\",\"quantity\":" + quantity + "}", session)
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions checkout(Cookie session, String key, long points) throws Exception {
        return mvc.perform(post("/api/v1/checkout").with(csrf()).cookie(session).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content("{\"pointsToRedeem\":" + points + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions confirm(Cookie session, JsonNode order, String outcome) throws Exception {
        return postJson("/api/v1/payments/" + order.path("payment").path("id").asText() + "/sandbox-confirmation",
                "{\"outcome\":\"" + outcome + "\"}", session);
    }

    private int available(UUID productId) {
        return inventory.findById(productId).orElseThrow().available();
    }
}
