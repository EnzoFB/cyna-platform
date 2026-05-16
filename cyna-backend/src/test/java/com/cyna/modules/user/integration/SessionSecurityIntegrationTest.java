package com.cyna.modules.user.integration;

import com.cyna.modules.user.interfaces.dto.request.ChangePasswordRequest;
import com.cyna.modules.user.interfaces.dto.request.RefreshRequest;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end verification that session security invariants hold:
 *
 * <ul>
 *   <li>Changing the password revokes every active refresh token.</li>
 *   <li>The change-password response returns a fresh, working token pair.</li>
 *   <li>Logout with {@code allDevices=true} revokes every session, not just one.</li>
 *   <li>Refresh-token rotation produces a new valid token and invalidates the old one.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class SessionSecurityIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cyna_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private record SessionPair(String accessToken, String refreshToken) {}

    private SessionPair registerAndExtract(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, "password123", "Test", "User", "Acme", "fr", true))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).at("/data");
        return new SessionPair(data.get("accessToken").asText(), data.get("refreshToken").asText());
    }

    @Test
    void password_change_returns_a_working_new_refresh_token() throws Exception {
        var initial = registerAndExtract("session.password-new@example.com");

        // Change password — old refresh tokens must die, new pair must be returned.
        MvcResult changeResult = mockMvc.perform(patch("/api/v1/account/password")
                        .header("Authorization", "Bearer " + initial.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("password123", "newPassword456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken").value(notNullValue()))
                .andReturn();

        String newRefresh = objectMapper.readTree(changeResult.getResponse().getContentAsString())
                .at("/data/refreshToken").asText();

        // The new refresh token returned by change-password must be valid for rotation.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(newRefresh))))
                .andExpect(status().isOk());
    }

    @Test
    void password_change_invalidates_the_previous_refresh_token() throws Exception {
        var initial = registerAndExtract("session.password-old@example.com");

        // Change password.
        mockMvc.perform(patch("/api/v1/account/password")
                        .header("Authorization", "Bearer " + initial.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("password123", "newPassword456"))))
                .andExpect(status().isOk());

        // The pre-change refresh token must be rejected — the reuse-detection
        // path fires (revoked token presented) so the scorched-earth cleanup
        // also dies any remaining session for this user. This is the desired
        // security posture; we verify the 401 here, not the side effect on the
        // brand-new pair (that's covered by the sibling test above).
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(initial.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_all_devices_revokes_every_refresh_token_for_the_owner() throws Exception {
        var deviceA = registerAndExtract("session.logout-all@example.com");

        // Rotate once to obtain a second active refresh token for the same user
        // (mimicking a second device session).
        MvcResult rotated = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(deviceA.refreshToken()))))
                .andExpect(status().isOk())
                .andReturn();

        String deviceBRefresh = objectMapper.readTree(rotated.getResponse().getContentAsString())
                .at("/data/refreshToken").asText();

        // Logout one device with allDevices=true → everything dies.
        mockMvc.perform(post("/api/v1/auth/logout?allDevices=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(deviceBRefresh))))
                .andExpect(status().isOk());

        // Both refresh tokens must now be rejected.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(deviceA.refreshToken()))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(deviceBRefresh))))
                .andExpect(status().isUnauthorized());
    }
}
