package az.buta.security;

import az.buta.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorizationIT extends IntegrationTest {

    @Test
    void anonymousUsersCannotUseAccountEndpoints() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code", is("AUTH_REQUIRED")));
        mvc.perform(get("/api/v1/me/favorites")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/cart/items").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"offerId\":\"00000000-0000-0000-0000-000000000001\",\"quantity\":1}")).andExpect(status().isUnauthorized());
    }

    @Test
    void customersCannotReachAdminEndpoints() throws Exception {
        Cookie customer = sessionFor(KAMRAN);
        perform(get("/api/v1/admin/offers"), customer).andExpect(status().isForbidden()).andExpect(jsonPath("$.code", is("FORBIDDEN")));
        postJson("/api/v1/admin/redemptions/redeem", "{\"code\":\"ABCD\"}", customer).andExpect(status().isForbidden());
        perform(get("/api/v1/admin/offers"), sessionFor(ADMIN)).andExpect(status().isOk());
    }

    @Test
    void stateChangingRequestsRequireCsrfToken() throws Exception {
        mvc.perform(post("/api/v1/auth/otp/request").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"x@buta.test\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code", is("CSRF_INVALID")));
    }

    @Test
    void foreignOriginsAreRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/otp/request").with(csrf()).header("Origin", "https://evil.example")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"x@buta.test\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code", is("ORIGIN_NOT_ALLOWED")));
        mvc.perform(post("/api/v1/auth/otp/request").with(csrf()).header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + uniqueEmail("origin") + "\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void usersCannotReadOtherUsersOrders() throws Exception {
        String kamranOrder = "70000000-0000-0000-0000-000000000003";
        perform(get("/api/v1/orders/" + kamranOrder), sessionFor(KAMRAN)).andExpect(status().isOk());
        perform(get("/api/v1/orders/" + kamranOrder), sessionFor(AYSEL)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        postJson("/api/v1/orders/" + kamranOrder + "/refund", "{}", sessionFor(AYSEL)).andExpect(status().isNotFound());
    }

    @Test
    void unknownFieldsAreRejectedToPreventMassAssignment() throws Exception {
        perform(patch("/api/v1/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Aysel\",\"lastName\":\"M\",\"username\":\"aysel\",\"roles\":[\"ADMIN\"]}"), sessionFor(AYSEL))
                .andExpect(status().isBadRequest());
        postJson("/api/v1/checkout/quote", "{\"pointsToRedeem\":0,\"price\":\"1.00\"}", sessionFor(AYSEL)).andExpect(status().isBadRequest());
    }

    @Test
    void securityHeadersArePresentAndErrorsHideInternals() throws Exception {
        mvc.perform(get("/api/v1/config"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("X-Request-Id"));
        mvc.perform(get("/api/v1/offers/%20")).andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void healthEndpointIsPublicWithoutDetails() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
    }
}
