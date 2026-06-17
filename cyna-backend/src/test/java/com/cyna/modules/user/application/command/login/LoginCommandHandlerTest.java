package com.cyna.modules.user.application.command.login;

import com.cyna.modules.user.application.model.LoginOutcome;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.application.port.OtpCodeGenerator;
import com.cyna.modules.user.application.port.OtpDeliveryPort;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.LoginOtpChallenge;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.TrustedDevice;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.model.Role;
import com.cyna.modules.user.domain.model.UserStatus;
import com.cyna.modules.user.domain.repository.LoginOtpChallengeRepository;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.repository.TrustedDeviceRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.OtpHasher;
import com.cyna.shared.application.RateLimiter;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import com.cyna.shared.infrastructure.security.HmacOtpHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginCommandHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private OtpCodeGenerator otpCodeGenerator;
    @Mock private OtpDeliveryPort otpDeliveryPort;
    @Mock private LoginOtpChallengeRepository loginOtpChallengeRepository;
    @Mock private TrustedDeviceRepository trustedDeviceRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private RateLimiter rateLimiter;

    private final OtpHasher otpHasher = new HmacOtpHasher("test-pepper-at-least-16-bytes-long");

    private LoginCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new LoginCommandHandler(
                userRepository,
                passwordHasher,
                otpCodeGenerator,
                otpDeliveryPort,
                loginOtpChallengeRepository,
                trustedDeviceRepository,
                refreshTokenRepository,
                jwtProvider,
                transactionRunner,
                rateLimiter,
                otpHasher,
                6,
                5,
                3,
                900,
                30
        );
    }

    private void allowThrottle() {
        when(rateLimiter.consume(any(), anyInt(), anyLong(), any(Instant.class)))
                .thenReturn(RateLimiter.RateLimitDecision.allowed(3, 2));
    }

    /**
     * Builds an ACTIVE (email-verified) customer. {@code User.register()} now
     * yields PENDING_VERIFICATION, which login rejects — these tests exercise
     * the credential/OTP paths, so they need a verified account.
     */
    private User activeUser(String email) {
        return User.reconstitute(
                UUID.randomUUID(), Email.of(email), HashedPassword.of("hashed"),
                "John", "Doe", null, Role.CUSTOMER, UserStatus.ACTIVE,
                Instant.now(), Instant.now());
    }

    @Test
    void should_create_login_challenge_successfully() {
        var command = new LoginCommand("test@example.com", "password123");
        var user = activeUser("test@example.com");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", user.getHashedPassword())).thenReturn(true);
        when(otpCodeGenerator.generateNumericCode(6)).thenReturn("123456");
        allowThrottle();

        Result<LoginOutcome> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInstanceOf(LoginOutcome.Challenge.class);
        var challenge = (LoginOutcome.Challenge) result.getValue();
        assertThat(challenge.challengeId()).isNotNull();
        assertThat(challenge.expiresInSeconds()).isEqualTo(300L);
        verify(loginOtpChallengeRepository).save(any(LoginOtpChallenge.class));
        // sendLoginOtp now takes the user's preferred lang as its 4th arg so the
        // mail template can be rendered in the right language. The default
        // LoginCommand(email, password) overload sets lang="fr" — assert on that.
        verify(otpDeliveryPort).sendLoginOtp(eq("test@example.com"), eq("123456"), any(), eq("fr"));
    }

    @Test
    void should_invalidate_previous_active_challenges_before_creating_new_one() {
        var command = new LoginCommand("test@example.com", "password123");
        var user = activeUser("test@example.com");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", user.getHashedPassword())).thenReturn(true);
        when(otpCodeGenerator.generateNumericCode(6)).thenReturn("123456");
        allowThrottle();

        handler.handle(command);

        // Ordering matters: the delete MUST run before the save, otherwise the
        // newly-created challenge would be wiped out by the same call.
        var order = inOrder(loginOtpChallengeRepository);
        order.verify(loginOtpChallengeRepository).deleteUnconsumedByUserId(user.getId());
        order.verify(loginOtpChallengeRepository).save(any(LoginOtpChallenge.class));
    }

    @Test
    void should_fail_when_email_not_found() {
        var command = new LoginCommand("unknown@example.com", "password123");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        Result<LoginOutcome> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid credentials");
    }

    @Test
    void should_fail_when_password_does_not_match() {
        var command = new LoginCommand("test@example.com", "wrongpassword");
        var user = activeUser("test@example.com");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrongpassword", user.getHashedPassword())).thenReturn(false);

        Result<LoginOutcome> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid credentials");
    }

    @Test
    void should_fail_with_email_not_verified_for_pending_account_with_right_password() {
        var command = new LoginCommand("test@example.com", "password123");
        // PENDING_VERIFICATION (as produced by self-service registration).
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("hashed"), "John", "Doe", "fr");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", user.getHashedPassword())).thenReturn(true);

        Result<LoginOutcome> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo(LoginCommandHandler.EMAIL_NOT_VERIFIED);
    }

    @Test
    void should_fail_with_invalid_credentials_for_pending_account_with_wrong_password() {
        var command = new LoginCommand("test@example.com", "wrongpassword");
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("hashed"), "John", "Doe", "fr");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrongpassword", user.getHashedPassword())).thenReturn(false);

        Result<LoginOutcome> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        // Pending account with a bad password reveals nothing beyond generic creds error.
        assertThat(result.getError()).isEqualTo("Invalid credentials");
    }

    @Test
    void should_reject_when_email_throttle_is_exhausted() {
        var command = new LoginCommand("test@example.com", "password123");
        var user = activeUser("test@example.com");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", user.getHashedPassword())).thenReturn(true);
        when(rateLimiter.consume(any(), anyInt(), anyLong(), any(Instant.class)))
                .thenReturn(RateLimiter.RateLimitDecision.rejected(3, 900));

        Result<LoginOutcome> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Too many OTP requests");
        // Crucially: no mail, no challenge, no DB write — the whole point of the throttle.
        verify(otpDeliveryPort, never()).sendLoginOtp(any(), any(), any(), any());
        verify(loginOtpChallengeRepository, never()).save(any());
        verify(loginOtpChallengeRepository, never()).deleteUnconsumedByUserId(any());
    }

    @Test
    void should_key_throttle_on_lowercased_stored_email_not_request_casing() {
        // Attacker varies the case in the request body (Test@Example.com,
        // TEST@example.com, …) to try to land in different buckets. The throttle
        // key MUST be derived from the user-stored email (canonical, lowercase),
        // not from request input, otherwise the protection is trivially bypassed.
        var command = new LoginCommand("Test@Example.COM", "password123");
        var user = activeUser("test@example.com");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", user.getHashedPassword())).thenReturn(true);
        when(otpCodeGenerator.generateNumericCode(6)).thenReturn("123456");
        allowThrottle();

        handler.handle(command);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter).consume(keyCaptor.capture(), anyInt(), anyLong(), any(Instant.class));
        assertThat(keyCaptor.getValue()).isEqualTo("login-otp-email:test@example.com");
    }

    // ---------- Trusted-device fast path ----------

    @Test
    void should_skip_otp_when_a_valid_trusted_device_cookie_is_presented() {
        var user = activeUser("test@example.com");
        String rawDeviceToken = "trusted-cookie-value";
        var trusted = TrustedDevice.reconstitute(
                UUID.randomUUID(),
                user.getId(),
                TokenHash.of(rawDeviceToken),
                Instant.now().plus(Duration.ofDays(15)),
                Instant.now().minus(Duration.ofDays(1)),
                Instant.now().minus(Duration.ofHours(2)),
                "Mozilla/5.0"
        );
        var command = new LoginCommand(
                "test@example.com", "password123", null, "fr", rawDeviceToken, "Mozilla/5.0");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", user.getHashedPassword())).thenReturn(true);
        when(trustedDeviceRepository.findByTokenHash(TokenHash.of(rawDeviceToken)))
                .thenReturn(Optional.of(trusted));
        when(jwtProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtProvider.getAccessTokenExpirationHours()).thenReturn(1L);
        when(jwtProvider.getRefreshTokenExpirationHours()).thenReturn(24L);

        Result<LoginOutcome> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInstanceOf(LoginOutcome.Authenticated.class);
        var auth = (LoginOutcome.Authenticated) result.getValue();
        assertThat(auth.tokens().accessToken()).isEqualTo("access-token");
        assertThat(auth.tokens().refreshToken()).isEqualTo("refresh-token");

        // No OTP path side-effects:
        verify(otpDeliveryPort, never()).sendLoginOtp(any(), any(), any(), any());
        verify(loginOtpChallengeRepository, never()).save(any());
        verify(rateLimiter, never()).consume(any(), anyInt(), anyLong(), any(Instant.class));

        // The renewed trusted device row was persisted (sliding expiry):
        ArgumentCaptor<TrustedDevice> deviceCaptor = ArgumentCaptor.forClass(TrustedDevice.class);
        verify(trustedDeviceRepository).save(deviceCaptor.capture());
        assertThat(deviceCaptor.getValue().tokenHash()).isEqualTo(TokenHash.of(rawDeviceToken));
        assertThat(deviceCaptor.getValue().expiresAt()).isAfter(trusted.expiresAt());
    }

    @Test
    void should_fall_back_to_otp_when_device_cookie_is_for_a_different_user() {
        var user = activeUser("test@example.com");
        String rawDeviceToken = "stale-cookie-from-another-account";
        var otherUserDevice = TrustedDevice.reconstitute(
                UUID.randomUUID(),
                UUID.randomUUID(), // different user
                TokenHash.of(rawDeviceToken),
                Instant.now().plus(Duration.ofDays(15)),
                Instant.now().minus(Duration.ofDays(1)),
                Instant.now().minus(Duration.ofHours(2)),
                null
        );
        var command = new LoginCommand(
                "test@example.com", "password123", null, "fr", rawDeviceToken, null);

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", user.getHashedPassword())).thenReturn(true);
        when(trustedDeviceRepository.findByTokenHash(TokenHash.of(rawDeviceToken)))
                .thenReturn(Optional.of(otherUserDevice));
        when(otpCodeGenerator.generateNumericCode(6)).thenReturn("123456");
        allowThrottle();

        Result<LoginOutcome> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInstanceOf(LoginOutcome.Challenge.class);
        verify(otpDeliveryPort).sendLoginOtp(any(), any(), any(), any());
    }

    @Test
    void should_fall_back_to_otp_when_device_cookie_is_expired() {
        var user = activeUser("test@example.com");
        String rawDeviceToken = "expired-cookie";
        var expired = TrustedDevice.reconstitute(
                UUID.randomUUID(),
                user.getId(),
                TokenHash.of(rawDeviceToken),
                Instant.now().minus(Duration.ofDays(1)),
                Instant.now().minus(Duration.ofDays(40)),
                Instant.now().minus(Duration.ofDays(40)),
                null
        );
        var command = new LoginCommand(
                "test@example.com", "password123", null, "fr", rawDeviceToken, null);

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", user.getHashedPassword())).thenReturn(true);
        when(trustedDeviceRepository.findByTokenHash(TokenHash.of(rawDeviceToken)))
                .thenReturn(Optional.of(expired));
        when(otpCodeGenerator.generateNumericCode(6)).thenReturn("123456");
        allowThrottle();

        Result<LoginOutcome> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInstanceOf(LoginOutcome.Challenge.class);
    }
}
