package az.buta.loyalty;

import az.buta.loyalty.domain.LedgerEntryRepository;
import az.buta.loyalty.domain.LoyaltyAccountRepository;
import az.buta.loyalty.domain.ReferralRepository;
import az.buta.loyalty.domain.ReferralStatus;
import az.buta.membership.MembershipService;
import az.buta.support.IntegrationTest;
import az.buta.user.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoyaltyIT extends IntegrationTest {

    @Autowired
    private MembershipService membership;

    @Autowired
    private LoyaltyAccountRepository accounts;

    @Autowired
    private LedgerEntryRepository ledger;

    @Autowired
    private ReferralRepository referrals;

    @Test
    void demoMemberShowsPointsTierAndNextBenefit() throws Exception {
        perform(get("/api/v1/me/loyalty"), sessionFor(KAMRAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", is("PLUS")))
                .andExpect(jsonPath("$.availablePoints", is(85)))
                .andExpect(jsonPath("$.tier.code", is("BRONZE")))
                .andExpect(jsonPath("$.nextTier.code", is("SILVER")))
                .andExpect(jsonPath("$.nextTier.pointsNeeded", is(15)));
        perform(get("/api/v1/me/loyalty"), sessionFor(AYSEL))
                .andExpect(jsonPath("$.plan", is("FREE")))
                .andExpect(jsonPath("$.earnsPoints", is(false)))
                .andExpect(jsonPath("$.qualifyingSpend.spent.amount", is("28.00")))
                .andExpect(jsonPath("$.qualifyingSpend.remaining.amount", is("72.00")));
    }

    @Test
    void freeMembersEarnNoPoints() throws Exception {
        Cookie session = newCustomer();
        JsonNode order = buy(session, "coffee-and-pastry", 1);
        assertThat(order.path("pointsEarned").asLong()).isZero();
        perform(get("/api/v1/me/loyalty"), session).andExpect(jsonPath("$.availablePoints", is(0)));
    }

    @Test
    void distinctPartnersTaskCompletesOnceAndIsReversedOnRefund() throws Exception {
        Cookie session = newCustomer();
        UUID userId = currentUserId(session);
        membership.assignPlus(userId, userId, null, "test");
        buy(session, "orbit-day-admission", 1);
        buy(session, "linen-evening-admission", 1);
        assertThat(task(session, "culture-explorer").path("progress").asInt()).isEqualTo(2);
        JsonNode third = buy(session, "the-paper-kite", 1);
        JsonNode completed = task(session, "culture-explorer");
        assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(completed.path("awarded").asBoolean()).isTrue();
        long afterTask = accounts.findById(userId).orElseThrow().availablePoints();

        buy(session, "evening-2d-screening", 1);
        assertThat(accounts.findById(userId).orElseThrow().availablePoints()).isEqualTo(afterTask + 10);

        postJson("/api/v1/orders/" + third.path("id").asText() + "/refund", "{}", session).andExpect(status().isOk());
        JsonNode afterRefund = task(session, "culture-explorer");
        assertThat(afterRefund.path("progress").asInt()).isEqualTo(3);
        assertThat(afterRefund.path("awarded").asBoolean()).isTrue();
    }

    @Test
    void bundleTaskAwardsOnlyOnceEvenWhenEvaluatedRepeatedly() throws Exception {
        Cookie session = newCustomer();
        UUID userId = currentUserId(session);
        membership.assignPlus(userId, userId, null, "test");
        JsonNode first = buy(session, "movie-and-coffee", 1);
        buy(session, "gallery-and-coffee", 1);
        long bundleAwards = ledger.findAll().stream()
                .filter(entry -> entry.userId().equals(userId) && entry.descriptionParams().getOrDefault("taskCode", "").equals("bundle-up"))
                .count();
        assertThat(bundleAwards).isEqualTo(1);
        assertThat(task(session, "bundle-up").path("status").asText()).isEqualTo("COMPLETED");
        assertThat(first.path("status").asText()).isEqualTo("FULFILLED");
    }

    @Test
    void attendingAnEventCompletesNightOutAfterPartnerRedemption() throws Exception {
        Cookie session = newCustomer();
        UUID userId = currentUserId(session);
        membership.assignPlus(userId, userId, null, "test");
        JsonNode order = buy(session, "planetarium-show", 1);
        assertThat(task(session, "night-out").path("progress").asInt()).isZero();
        String code = order.path("items").get(0).path("vouchers").get(0).path("code").asText();
        Cookie admin = sessionFor(ADMIN);
        postJson("/api/v1/admin/redemptions/redeem", "{\"code\":\"" + code + "\"}", admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("REDEEMED"))).andExpect(jsonPath("$.changed", is(true)));
        postJson("/api/v1/admin/redemptions/redeem", "{\"code\":\"" + code + "\"}", admin).andExpect(jsonPath("$.changed", is(false)));
        assertThat(task(session, "night-out").path("status").asText()).isEqualTo("COMPLETED");
        perform(get("/api/v1/orders/" + order.path("id").asText()), session).andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void referralQualifiesOnRefereesFirstPurchaseAndRejectsSelfReferral() throws Exception {
        User referrer = createUser("referrer", "ip-a");
        membership.assignPlus(referrer.getId(), referrer.getId(), null, "test");
        Cookie self = sessionFor(referrer.email());
        Cookie referee = registerAndSignIn(uniqueEmail("referee"), null);
        postJson("/api/v1/auth/complete-profile", "{\"firstName\":\"R\",\"lastName\":\"E\",\"username\":\"ref" + suffix()
                + "\",\"referralCode\":\"NOPE1234\"}", referee).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("INVALID_REFERRAL_CODE")));
        postJson("/api/v1/auth/complete-profile", "{\"firstName\":\"R\",\"lastName\":\"E\",\"username\":\"ref" + suffix()
                + "\",\"referralCode\":\"" + referrer.referralCode() + "\"}", referee).andExpect(status().isOk());
        UUID refereeId = currentUserId(referee);
        assertThat(referrals.findByRefereeUserId(refereeId).orElseThrow().status()).isEqualTo(ReferralStatus.PENDING);

        buy(referee, "festival-day-pass", 1);
        ReferralStatus status = referrals.findByRefereeUserId(refereeId).orElseThrow().status();
        assertThat(status).isIn(ReferralStatus.QUALIFIED, ReferralStatus.UNDER_REVIEW);
        if (status == ReferralStatus.UNDER_REVIEW) {
            UUID referralId = referrals.findByRefereeUserId(refereeId).orElseThrow().getId();
            postJson("/api/v1/admin/referrals/" + referralId + "/approve", "{}", sessionFor(ADMIN)).andExpect(status().isOk());
        }
        assertThat(task(self, "invite-a-friend").path("awarded").asBoolean()).isTrue();
    }

    @Test
    void suspiciousReferralsFromTheSameNetworkGoToReview() throws Exception {
        User referrer = createUser("netref", "same-network");
        User referee = createUser("netee", "same-network");
        membership.assignPlus(referrer.getId(), referrer.getId(), null, "test");
        az.buta.loyalty.domain.Referral referral = referrals.save(new az.buta.loyalty.domain.Referral(referrer.getId(), referee.getId(),
                referrer.referralCode(), Instant.now()));
        buy(sessionFor(referee.email()), "festival-day-pass", 1);
        assertThat(referrals.findById(referral.getId()).orElseThrow().status()).isEqualTo(ReferralStatus.UNDER_REVIEW);
        perform(get("/api/v1/admin/referrals"), sessionFor(ADMIN)).andExpect(status().isOk());
    }

    private JsonNode task(Cookie session, String code) throws Exception {
        JsonNode tasks = json(perform(get("/api/v1/me/loyalty/tasks"), session));
        for (JsonNode task : tasks) {
            if (task.path("code").asText().equals(code)) {
                return task;
            }
        }
        throw new AssertionError("Task not found: " + code);
    }

    private User createUser(String prefix, String ipHash) {
        Instant now = Instant.now();
        User user = User.register(uniqueEmail(prefix), ("RF" + UUID.randomUUID().toString().replace("-", "")).substring(0, 8).toUpperCase(), ipHash, now);
        user.completeProfile("Test", "User", prefix + suffix(), now);
        return users.save(user);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
