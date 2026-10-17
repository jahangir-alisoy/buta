package az.buta.user;

import az.buta.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileIT extends IntegrationTest {

    @Test
    void userUpdatesProfileAndPreferences() throws Exception {
        Cookie session = newCustomer();
        perform(patch("/api/v1/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Günay\",\"lastName\":\"Rzayeva\",\"username\":\"gunay" + System.nanoTime() % 100000
                        + "\",\"phone\":\"+994501234567\"}"), session)
                .andExpect(status().isOk()).andExpect(jsonPath("$.firstName", is("Günay"))).andExpect(jsonPath("$.phone", is("+994501234567")));
        perform(patch("/api/v1/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"username\":\"abc\",\"phone\":\"12345\"}"), session)
                .andExpect(status().isBadRequest());
        perform(put("/api/v1/me/preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"locale\":\"en\",\"theme\":\"DARK\",\"personalizationEnabled\":true,"
                        + "\"preferredCategoryIds\":[\"10000000-0000-0000-0000-000000000002\"],\"budgetMin\":5,\"budgetMax\":30}"), session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("DARK")))
                .andExpect(jsonPath("$.personalizationEnabled", is(true)));
        perform(get("/api/v1/me"), session).andExpect(jsonPath("$.locale", is("en"))).andExpect(jsonPath("$.theme", is("DARK")));
    }

    @Test
    void avatarUploadIsValidatedAndReencoded() throws Exception {
        Cookie session = newCustomer();
        MockMultipartFile fake = new MockMultipartFile("file", "avatar.png", "image/png", "<svg onload=alert(1)>".getBytes());
        mvc.perform(multipart("/api/v1/me/avatar").file(fake).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }).with(csrf()).cookie(session))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("FILE_TYPE_NOT_ALLOWED")));

        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        MockMultipartFile valid = new MockMultipartFile("file", "avatar.png", "image/png", png.toByteArray());
        mvc.perform(multipart("/api/v1/me/avatar").file(valid).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }).with(csrf()).cookie(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl", startsWith("/api/v1/me/avatar")));

        MvcResult avatar = perform(get("/api/v1/me/avatar"), session)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/jpeg")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn();
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(avatar.getResponse().getContentAsByteArray()));
        assertThat(stored.getWidth()).isEqualTo(256);
        assertThat(stored.getHeight()).isEqualTo(256);
        perform(get("/api/v1/me/avatar"), sessionFor(AYSEL)).andExpect(status().isNotFound());
    }

    @Test
    void emailChangeRequiresVerificationOfTheNewAddress() throws Exception {
        Cookie session = newCustomer();
        String newEmail = uniqueEmail("changed");
        postJson("/api/v1/me/email-change", "{\"newEmail\":\"" + newEmail + "\"}", session).andExpect(status().isAccepted());
        perform(get("/api/v1/me"), session).andExpect(jsonPath("$.email", startsWith("customer.")));
        MvcResult confirmed = postJson("/api/v1/me/email-change/confirm", "{\"code\":\"" + latestCode(newEmail) + "\"}", session)
                .andExpect(status().isOk()).andExpect(jsonPath("$.email", is(newEmail))).andReturn();
        Cookie rotated = confirmed.getResponse().getCookie("buta_session");
        assertThat(rotated.getValue()).isNotEqualTo(session.getValue());
        perform(get("/api/v1/me"), session).andExpect(status().isUnauthorized());
        perform(get("/api/v1/me"), rotated).andExpect(status().isOk());
    }

    @Test
    void favoritesAreIdempotentAndListed() throws Exception {
        Cookie session = newCustomer();
        String offer = offerId("the-glass-orchard").toString();
        perform(put("/api/v1/me/favorites/" + offer), session).andExpect(status().isNoContent());
        perform(put("/api/v1/me/favorites/" + offer), session).andExpect(status().isNoContent());
        perform(get("/api/v1/me/favorites"), session).andExpect(jsonPath("$.length()", is(1))).andExpect(jsonPath("$[0].favorite", is(true)));
        perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/me/favorites/" + offer), session)
                .andExpect(status().isNoContent());
        perform(get("/api/v1/me/favorites"), session).andExpect(jsonPath("$.length()", is(0)));
    }
}
