package com.cyna.modules.user.integration;

import com.cyna.modules.user.interfaces.dto.request.LoginRequest;
import com.cyna.modules.user.interfaces.dto.request.RefreshRequest;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    @Container
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------------ helpers

    private MvcResult registerUser(String email, String password, String firstName, String lastName) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, password, firstName, lastName, "fr", true))))
                .andReturn();
    }

    private String registerAndExtractAccessToken(String email) throws Exception {
        MvcResult result = registerUser(email, "password123", "Test", "User");
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
    }

    private String registerAndExtractRefreshToken(String email) throws Exception {
        MvcResult result = registerUser(email, "password123", "Test", "User");
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/refreshToken").asText();
    }

    // ==================================================================
    // POST /api/v1/auth/register
    // ==================================================================

    @Nested
    class Register {

        @Test
        void should_create_account_and_return_jwt_tokens() throws Exception {
            var request = new RegisterRequest("reg.ok@example.com", "password123", "Alice", "Martin", "fr", true);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value(notNullValue()))
                    .andExpect(jsonPath("$.data.refreshToken").value(notNullValue()))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").isNumber());
        }

        @Test
        void should_reject_duplicate_email() throws Exception {
            var email = "reg.dup@example.com";
            registerUser(email, "password123", "First", "User");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest(email, "password123", "Second", "User", "fr", true))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATION"));
        }

        @Test
        void should_reject_invalid_email_format() throws Exception {
            var request = new RegisterRequest("not-an-email", "password123", "Alice", "Martin", "fr", true);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.error.details[0].field").value("email"));
        }

        @Test
        void should_reject_password_too_short() throws Exception {
            var request = new RegisterRequest("reg.shortpwd@example.com", "short", "Alice", "Martin", "fr", true);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.error.details[0].field").value("password"));
        }

        @Test
        void should_reject_missing_required_fields() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.error.details").isArray());
        }
    }

    // ==================================================================
    // POST /api/v1/auth/login
    // ==================================================================

    @Nested
    class Login {

        @Test
        void should_initiate_otp_challenge_on_valid_credentials() throws Exception {
            // POST /auth/login no longer returns tokens directly — it starts the
            // OTP step by returning a challenge id. Tokens are issued only by
            // POST /auth/login/verify-otp with the right code. Asserting the
            // challenge handshake is enough here; the OTP completion path has
            // its own VerifyLoginOtpCommandHandlerTest unit coverage.
            var email = "login.ok@example.com";
            registerUser(email, "password123", "Bob", "Dupont");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(email, "password123", "fr"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.challengeId").value(notNullValue()))
                    .andExpect(jsonPath("$.data.expiresInSeconds").isNumber());
        }

        @Test
        void should_reject_wrong_password() throws Exception {
            var email = "login.badpwd@example.com";
            registerUser(email, "password123", "Bob", "Dupont");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(email, "wrongpassword", "fr"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        void should_reject_unknown_email() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest("ghost@example.com", "password123", "fr"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    // ==================================================================
    // POST /api/v1/auth/refresh
    // ==================================================================

    @Nested
    class TokenRefresh {

        @Test
        void should_issue_new_token_pair() throws Exception {
            String refreshToken = registerAndExtractRefreshToken("refresh.ok@example.com");

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshRequest(refreshToken))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value(notNullValue()))
                    .andExpect(jsonPath("$.data.refreshToken").value(notNullValue()))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        void should_reject_unknown_refresh_token() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshRequest("00000000-0000-0000-0000-000000000000"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        void should_reject_reused_refresh_token() throws Exception {
            String refreshToken = registerAndExtractRefreshToken("refresh.reuse@example.com");

            // First use — OK
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshRequest(refreshToken))))
                    .andExpect(status().isOk());

            // Reuse of the same token — must be rejected (rotation invalidates old token)
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshRequest(refreshToken))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    // ==================================================================
    // POST /api/v1/auth/logout
    // ==================================================================

    @Nested
    class Logout {

        @Test
        void should_revoke_refresh_token() throws Exception {
            String refreshToken = registerAndExtractRefreshToken("logout.ok@example.com");

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshRequest(refreshToken))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void should_prevent_token_refresh_after_logout() throws Exception {
            String refreshToken = registerAndExtractRefreshToken("logout.revoked@example.com");

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshRequest(refreshToken))))
                    .andExpect(status().isOk());

            // Attempt to refresh with revoked token
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshRequest(refreshToken))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    // ==================================================================
    // GET /api/v1/account
    // ==================================================================

    @Nested
    class Account {

        @Test
        void should_return_authenticated_user_profile() throws Exception {
            String accessToken = registerAndExtractAccessToken("account.ok@example.com");

            mockMvc.perform(get("/api/v1/account")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").isNotEmpty())
                    .andExpect(jsonPath("$.data.email").value("account.ok@example.com"))
                    .andExpect(jsonPath("$.data.firstName").value("Test"))
                    .andExpect(jsonPath("$.data.lastName").value("User"))
                    .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
        }

        @Test
        void should_reject_unauthenticated_request() throws Exception {
            mockMvc.perform(get("/api/v1/account"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        void should_reject_tampered_access_token() throws Exception {
            mockMvc.perform(get("/api/v1/account")
                            .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.tampered.signature"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
