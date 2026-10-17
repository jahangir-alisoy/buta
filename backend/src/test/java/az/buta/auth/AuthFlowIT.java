package az.buta.auth;

import az.buta.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class AuthFlowIT extends IntegrationTest {

    @Test
    void newUserRegistersWithOtpAndCompletesProfile(CapturedOutput output) throws Exception {
        String email = uniqueEmail("new");
        MvcResult request = postJson("/api/v1/auth/otp/request", "{\"email\":\"" + email.toUpperCase() + "\"}", null)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.resendAfterSeconds", is(60)))
                .andReturn();
        String code = latestCode(email);
        assertThat(request.getResponse().getContentAsString()).doesNotContain(code);

        MvcResult verify = postJson("/api/v1/auth/otp/verify", "{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep", is("COMPLETE_PROFILE")))
                .andReturn();
        Cookie session = verify.getResponse().getCookie("buta_session");
        assertThat(session).isNotNull();
        assertThat(session.isHttpOnly()).isTrue();
        assertThat(verify.getResponse().getHeader("Set-Cookie")).contains("SameSite=Lax");

        perform(get("/api/v1/cart"), session).andExpect(status().isForbidden()).andExpect(jsonPath("$.code", is("PROFILE_INCOMPLETE")));
        String username = "user" + System.nanoTime() % 100000;
        postJson("/api/v1/auth/complete-profile", "{\"firstName\":\"Nigar\",\"lastName\":\"Quliyeva\",\"username\":\"" + username + "\"}", session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.username", is(username)));
        perform(get("/api/v1/cart"), session).andExpect(status().isOk());
        assertThat(output.getAll()).doesNotContain(code);
    }

    @Test
    void codeIsSingleUse() throws Exception {
        String email = uniqueEmail("single");
        postJson("/api/v1/auth/otp/request", "{\"email\":\"" + email + "\"}", null).andExpect(status().isAccepted());
        String code = latestCode(email);
        String body = "{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}";
        postJson("/api/v1/auth/otp/verify", body, null).andExpect(status().isOk());
        postJson("/api/v1/auth/otp/verify", body, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("OTP_INVALID")));
    }

    @Test
    void expiredCodeIsRejected() throws Exception {
        String email = uniqueEmail("expired");
        postJson("/api/v1/auth/otp/request", "{\"email\":\"" + email + "\"}", null).andExpect(status().isAccepted());
        String code = latestCode(email);
        clock.advance(Duration.ofMinutes(11));
        postJson("/api/v1/auth/otp/verify", "{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}", null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("OTP_INVALID")));
    }

    @Test
    void tooManyWrongAttemptsLockTheChallenge() throws Exception {
        String email = uniqueEmail("locked");
        postJson("/api/v1/auth/otp/request", "{\"email\":\"" + email + "\"}", null).andExpect(status().isAccepted());
        String code = latestCode(email);
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int attempt = 0; attempt < 4; attempt++) {
            postJson("/api/v1/auth/otp/verify", "{\"email\":\"" + email + "\",\"code\":\"" + wrong + "\"}", null)
                    .andExpect(status().isBadRequest());
        }
        postJson("/api/v1/auth/otp/verify", "{\"email\":\"" + email + "\",\"code\":\"" + wrong + "\"}", null)
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code", is("OTP_LOCKED")));
        postJson("/api/v1/auth/otp/verify", "{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}", null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void newRequestSupersedesPreviousCodeAndCooldownApplies() throws Exception {
        String email = uniqueEmail("resend");
        postJson("/api/v1/auth/otp/request", "{\"email\":\"" + email + "\"}", null).andExpect(status().isAccepted());
        String first = latestCode(email);
        postJson("/api/v1/auth/otp/request", "{\"email\":\"" + email + "\"}", null)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", notNullValue()));
        clock.advance(Duration.ofSeconds(61));
        postJson("/api/v1/auth/otp/request", "{\"email\":\"" + email + "\"}", null).andExpect(status().isAccepted());
        String second = latestCode(email);
        if (!first.equals(second)) {
            postJson("/api/v1/auth/otp/verify", "{\"email\":\"" + email + "\",\"code\":\"" + first + "\"}", null)
                    .andExpect(status().isBadRequest());
        }
        postJson("/api/v1/auth/otp/verify", "{\"email\":\"" + email + "\",\"code\":\"" + second + "\"}", null).andExpect(status().isOk());
    }

    @Test
    void perEmailRequestLimitIsEnforced() throws Exception {
        String email = uniqueEmail("limit");
        for (int request = 0; request < 5; request++) {
            postJson("/api/v1/auth/otp/request", "{\"email\":\"" + email + "\"}", null).andExpect(status().isAccepted());
            clock.advance(Duration.ofSeconds(61));
        }
        postJson("/api/v1/auth/otp/request", "{\"email\":\"" + email + "\"}", null).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code", is("RATE_LIMITED")));
    }

    @Test
    void perIpRequestLimitIsEnforced() throws Exception {
        for (int request = 0; request < 20; request++) {
            postJson("/api/v1/auth/otp/request", "{\"email\":\"" + uniqueEmail("ip") + "\"}", null).andExpect(status().isAccepted());
        }
        postJson("/api/v1/auth/otp/request", "{\"email\":\"" + uniqueEmail("ip") + "\"}", null).andExpect(status().isTooManyRequests());
    }

    @Test
    void responsesAreIdenticalForKnownAndUnknownEmails() throws Exception {
        String known = postJson("/api/v1/auth/otp/request", "{\"email\":\"" + LEYLA + "\"}", null)
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String unknown = postJson("/api/v1/auth/otp/request", "{\"email\":\"" + uniqueEmail("nobody") + "\"}", null)
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertThat(known).isEqualTo(unknown);
    }

    @Test
    void sessionEndpointReportsStateAndLogoutRevokesSession() throws Exception {
        mvc.perform(get("/api/v1/auth/session")).andExpect(status().isOk()).andExpect(jsonPath("$.authenticated", is(false)));
        Cookie session = sessionFor(AYSEL);
        perform(get("/api/v1/auth/session"), session).andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.user.membership", is("FREE")));
        postJson("/api/v1/auth/logout", "{}", session).andExpect(status().isNoContent());
        perform(get("/api/v1/me"), session).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code", is("AUTH_REQUIRED")));
    }

    @Test
    void logoutEverywhereRevokesAllSessions() throws Exception {
        Cookie first = sessionFor(LEYLA);
        Cookie second = sessionFor(LEYLA);
        postJson("/api/v1/auth/logout-all", "{}", first).andExpect(status().isNoContent());
        perform(get("/api/v1/me"), second).andExpect(status().isUnauthorized());
    }

    @Test
    void sessionExpiresAfterIdleTimeout() throws Exception {
        Cookie session = sessionFor(AYSEL);
        perform(get("/api/v1/me"), session).andExpect(status().isOk());
        clock.advance(Duration.ofDays(8));
        perform(get("/api/v1/me"), session).andExpect(status().isUnauthorized());
    }

    @Test
    void bootstrapAdminEmailReceivesAdminRoleOnSignIn() throws Exception {
        Cookie session = registerAndSignIn("bootstrap.admin@buta.test", "bootstrapadmin");
        perform(get("/api/v1/auth/session"), session).andExpect(jsonPath("$.user.roles.length()", is(2)));
        perform(get("/api/v1/admin/categories"), session).andExpect(status().isOk());
    }

    @Test
    void usernameRulesAreEnforced() throws Exception {
        Cookie session = registerAndSignIn(uniqueEmail("rules"), null);
        postJson("/api/v1/auth/complete-profile", "{\"firstName\":\"A\",\"lastName\":\"B\",\"username\":\"admin\"}", session)
                .andExpect(status().isBadRequest());
        postJson("/api/v1/auth/complete-profile", "{\"firstName\":\"A\",\"lastName\":\"B\",\"username\":\"kamran\"}", session)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errors[0].field", is("username")));
    }

    @Test
    void validationMessagesAreLocalized() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/otp/request")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .header("Accept-Language", "en").contentType("application/json").content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", is("This field cannot be empty.")));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/otp/request")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .header("Accept-Language", "az").contentType("application/json").content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", is("Bu sahə boş ola bilməz.")));
    }
}
