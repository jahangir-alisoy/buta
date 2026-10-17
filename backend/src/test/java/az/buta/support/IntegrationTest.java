package az.buta.support;

import az.buta.auth.SessionService;
import az.buta.catalog.OfferCatalog;
import az.buta.catalog.OfferPopularityService;
import az.buta.notification.EmailMessage;
import az.buta.notification.InMemoryEmailSender;
import az.buta.security.InMemoryRateLimiter;
import az.buta.user.domain.User;
import az.buta.user.domain.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
public abstract class IntegrationTest {

    public static final String KAMRAN = "kamran.plus@buta.test";
    public static final String LEYLA = "leyla.plus@buta.test";
    public static final String AYSEL = "aysel.free@buta.test";
    public static final String ADMIN = "admin@buta.test";

    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("buta")
            .withUsername("buta")
            .withPassword("buta");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected InMemoryEmailSender mailbox;

    @Autowired
    protected InMemoryRateLimiter rateLimiter;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected SessionService sessionService;

    @Autowired
    protected UserRepository users;

    @Autowired
    protected OfferCatalog offerCatalog;

    @Autowired
    protected OfferPopularityService popularity;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void resetSharedState() {
        clock.reset();
        mailbox.clear();
        rateLimiter.reset();
        offerCatalog.evict();
        popularity.evict();
    }

    protected Cookie sessionFor(String email) {
        User user = users.findByEmail(email).orElseThrow();
        return new Cookie("buta_session", sessionService.open(user, "integration-test", "127.0.0.1").token());
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@buta.test";
    }

    protected String latestCode(String email) {
        List<EmailMessage> messages = mailbox.messagesFor(email);
        if (messages.isEmpty()) {
            throw new AssertionError("No email for " + email);
        }
        Matcher matcher = CODE.matcher(messages.getFirst().textBody());
        if (!matcher.find()) {
            throw new AssertionError("No code in email");
        }
        return matcher.group(1);
    }

    protected Cookie registerAndSignIn(String email, String username) throws Exception {
        postJson("/api/v1/auth/otp/request", "{\"email\":\"" + email + "\"}", null).andExpect(status().isAccepted());
        MvcResult result = postJson("/api/v1/auth/otp/verify", "{\"email\":\"" + email + "\",\"code\":\"" + latestCode(email) + "\"}", null)
                .andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie("buta_session");
        if (username != null) {
            postJson("/api/v1/auth/complete-profile",
                    "{\"firstName\":\"Test\",\"lastName\":\"User\",\"username\":\"" + username + "\"}", cookie).andExpect(status().isOk());
        }
        return cookie;
    }

    protected ResultActions postJson(String path, String body, Cookie cookie) throws Exception {
        return mvc.perform(withCookie(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body), cookie));
    }

    protected ResultActions perform(MockHttpServletRequestBuilder builder, Cookie cookie) throws Exception {
        return mvc.perform(withCookie(builder.with(csrf()), cookie));
    }

    protected JsonNode json(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    protected UUID offerId(String slug) throws Exception {
        JsonNode node = json(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/offers/" + slug)));
        return UUID.fromString(node.path("offer").path("id").asText());
    }

    protected Cookie newCustomer() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return registerAndSignIn(uniqueEmail("customer"), "customer" + suffix);
    }

    protected UUID currentUserId(Cookie session) throws Exception {
        return UUID.fromString(json(perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/me"), session))
                .path("id").asText());
    }

    protected JsonNode buy(Cookie session, String slug, int quantity) throws Exception {
        postJson("/api/v1/cart/items", "{\"offerId\":\"" + offerId(slug) + "\",\"quantity\":" + quantity + "}", session)
                .andExpect(status().isOk());
        JsonNode order = json(mvc.perform(post("/api/v1/checkout").with(csrf()).cookie(session).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content("{\"pointsToRedeem\":0}")).andExpect(status().isCreated()));
        return json(postJson("/api/v1/payments/" + order.path("payment").path("id").asText() + "/sandbox-confirmation",
                "{\"outcome\":\"SUCCEEDED\"}", session).andExpect(status().isOk()));
    }

    private static MockHttpServletRequestBuilder withCookie(MockHttpServletRequestBuilder builder, Cookie cookie) {
        return cookie == null ? builder : builder.cookie(cookie);
    }
}
