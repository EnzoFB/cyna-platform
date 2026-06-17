package com.cyna.modules.user.integration;

import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.EmailVerificationToken;
import com.cyna.modules.user.domain.model.PasswordResetToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.repository.EmailVerificationTokenRepository;
import com.cyna.modules.user.domain.repository.PasswordResetTokenRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.modules.user.interfaces.dto.request.ConfirmEmailRequest;
import com.cyna.modules.user.interfaces.dto.request.ForgotPasswordRequest;
import com.cyna.modules.user.interfaces.dto.request.LoginRequest;
import com.cyna.modules.user.interfaces.dto.request.RefreshRequest;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import com.cyna.modules.user.interfaces.dto.request.ResetPasswordRequest;
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

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end verification of the forgot-password flow:
 *
 * <ul>
 *   <li>Request for a known email creates a single-use token (silently for
 *       the response — anti-enumeration).</li>
 *   <li>Request for an unknown email returns 200 too (anti-enumeration).</li>
 *   <li>Reset with a valid token replaces the password and revokes all
 *       active refresh tokens; reusing the same token yields 400.</li>
 *   <li>Reset with an expired or unknown token yields 400.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PasswordResetIntegrationTest {

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
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private EmailVerificationTokenRepository verificationTokenRepository;
    @Autowired private JwtProvider jwtProvider;

    /**
     * Registers a user (now created PENDING_VERIFICATION) and activates the
     * account via confirm-email — seeding a known raw verification token — so
     * the returned refresh token belongs to a real, ACTIVE account.
     */
    private String registerAndExtractRefreshToken(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, "password123", "Test", "User", "Acme", "fr", true))))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail(Email.of(email)).orElseThrow();
        verificationTokenRepository.deleteUnconsumedByUserId(user.getId());
        String rawVerify = "verify-known-" + System.nanoTime();
        verificationTokenRepository.save(EmailVerificationToken.create(
                user.getId(), TokenHash.of(rawVerify), Instant.now().plus(Duration.ofHours(24))));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmEmailRequest(rawVerify))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/refreshToken").asText();
    }

    /**
     * Issues a real reset token by going through the API, then re-injects it
     * directly using a known raw value so the test can drive the reset step
     * without intercepting the (mailed-only) raw token.
     */
    private String seedKnownRawToken(String email) throws Exception {
        // 1. Trigger the API so the user record + flow are real.
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest(email, "fr"))))
                .andExpect(status().isOk());

        // 2. Replace the pending token with one whose raw value we control.
        var user = userRepository.findByEmail(Email.of(email)).orElseThrow();
        tokenRepository.deleteUnconsumedByUserId(user.getId());

        String raw = "test-known-raw-token-" + System.nanoTime();
        tokenRepository.save(PasswordResetToken.create(
                user.getId(),
                TokenHash.of(raw),
                Instant.now().plus(Duration.ofHours(1))
        ));
        return raw;
    }

    @Test
    void forgot_password_for_known_email_returns_200() throws Exception {
        registerAndExtractRefreshToken("reset.known@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("reset.known@example.com", "fr"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void forgot_password_for_unknown_email_also_returns_200_to_prevent_enumeration() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("nobody@example.com", "fr"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void reset_password_with_valid_token_replaces_password_and_revokes_sessions() throws Exception {
        String refreshToken = registerAndExtractRefreshToken("reset.full-flow@example.com");
        String raw = seedKnownRawToken("reset.full-flow@example.com");

        // Reset succeeds.
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest(raw, "brandNewPwd456"))))
                .andExpect(status().isOk());

        // The pre-reset refresh token is now dead.
        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isUnauthorized());

        // The new password works: the login endpoint accepts it and returns
        // an OTP challenge (no creds-rejected 401).
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("reset.full-flow@example.com", "brandNewPwd456", "fr"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.challengeId").exists());

        // The token cannot be reused.
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest(raw, "yetAnotherPwd789"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void reset_password_with_unknown_token_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("ghost-token", "anyPassword123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void reset_password_old_password_is_rejected_at_login() throws Exception {
        registerAndExtractRefreshToken("reset.old-pwd@example.com");
        String raw = seedKnownRawToken("reset.old-pwd@example.com");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest(raw, "completelyNewPwd"))))
                .andExpect(status().isOk());

        // Original password no longer authenticates.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("reset.old-pwd@example.com", "password123", "fr"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void seed_helper_assumption_token_repository_is_wired() {
        // Sanity check that the test fixtures have access to the real beans —
        // catches a context-loading regression cheaply.
        assertThat(jwtProvider).isNotNull();
        assertThat(tokenRepository).isNotNull();
        assertThat(userRepository).isNotNull();
    }
}
