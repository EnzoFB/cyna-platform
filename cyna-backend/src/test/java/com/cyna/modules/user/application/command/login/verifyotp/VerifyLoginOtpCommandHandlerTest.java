package com.cyna.modules.user.application.command.login.verifyotp;

import com.cyna.modules.user.application.model.VerifyOtpOutcome;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.LoginOtpChallenge;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.TrustedDevice;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.LoginOtpChallengeRepository;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.repository.TrustedDeviceRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.OtpHasher;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyLoginOtpCommandHandlerTest {

    @Mock private LoginOtpChallengeRepository loginOtpChallengeRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TrustedDeviceRepository trustedDeviceRepository;
    @Mock private JwtProvider jwtProvider;

    private final OtpHasher otpHasher = new HmacOtpHasher("test-pepper-at-least-16-bytes-long");

    private VerifyLoginOtpCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new VerifyLoginOtpCommandHandler(
                loginOtpChallengeRepository,
                userRepository,
                refreshTokenRepository,
                trustedDeviceRepository,
                jwtProvider,
                transactionRunner,
                otpHasher,
                30
        );
    }

    private LoginOtpChallenge pendingChallenge(UUID challengeId, UUID userId, String code, int attempts) {
        return new LoginOtpChallenge(
                challengeId,
                userId,
                otpHasher.hash(code),
                Instant.now().plus(Duration.ofMinutes(5)),
                false,
                Instant.now(),
                null,
                attempts
        );
    }

    @Test
    void should_verify_otp_and_return_tokens_with_device_token() {
        UUID challengeId = UUID.randomUUID();
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("hashed"), "John", "Doe", "fr");
        var challenge = pendingChallenge(challengeId, user.getId(), "123456", 0);
        var command = new VerifyLoginOtpCommand(challengeId, "123456", "Mozilla/5.0 (Test)");

        when(loginOtpChallengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtProvider.generateAccessToken(user)).thenReturn("access-token");
        // generateRefreshToken is called twice: once for refresh token, once for device token.
        when(jwtProvider.generateRefreshToken()).thenReturn("refresh-token", "device-token");
        when(jwtProvider.getAccessTokenExpirationHours()).thenReturn(1L);
        when(jwtProvider.getRefreshTokenExpirationHours()).thenReturn(24L);

        Result<VerifyOtpOutcome> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().tokens().accessToken()).isEqualTo("access-token");
        assertThat(result.getValue().tokens().refreshToken()).isEqualTo("refresh-token");
        assertThat(result.getValue().trustedDeviceToken()).isEqualTo("device-token");
        verify(loginOtpChallengeRepository).save(any(LoginOtpChallenge.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        ArgumentCaptor<TrustedDevice> deviceCaptor = ArgumentCaptor.forClass(TrustedDevice.class);
        verify(trustedDeviceRepository).save(deviceCaptor.capture());
        assertThat(deviceCaptor.getValue().userId()).isEqualTo(user.getId());
        assertThat(deviceCaptor.getValue().tokenHash()).isEqualTo(TokenHash.of("device-token"));
        assertThat(deviceCaptor.getValue().userAgent()).isEqualTo("Mozilla/5.0 (Test)");
        assertThat(deviceCaptor.getValue().expiresAt()).isAfter(Instant.now().plus(Duration.ofDays(29)));
    }

    @Test
    void should_fail_when_challenge_not_found() {
        UUID challengeId = UUID.randomUUID();
        var command = new VerifyLoginOtpCommand(challengeId, "123456");

        when(loginOtpChallengeRepository.findById(challengeId)).thenReturn(Optional.empty());

        Result<VerifyOtpOutcome> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid OTP challenge");
        verify(loginOtpChallengeRepository, never()).save(any());
        verify(trustedDeviceRepository, never()).save(any());
    }

    @Test
    void should_fail_with_invalid_code_and_persist_incremented_attempts() {
        UUID challengeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var challenge = pendingChallenge(challengeId, userId, "123456", 0);
        var command = new VerifyLoginOtpCommand(challengeId, "654321");

        when(loginOtpChallengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        Result<VerifyOtpOutcome> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid OTP code");

        ArgumentCaptor<LoginOtpChallenge> saved = ArgumentCaptor.forClass(LoginOtpChallenge.class);
        verify(loginOtpChallengeRepository).save(saved.capture());
        assertThat(saved.getValue().attempts()).isEqualTo(1);
        assertThat(saved.getValue().consumed()).isFalse();
        verify(trustedDeviceRepository, never()).save(any());
    }

    @Test
    void should_lock_challenge_when_max_attempts_reached() {
        UUID challengeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        // Already at MAX - 1; one more wrong submission tips it into locked state.
        var challenge = pendingChallenge(challengeId, userId, "123456", LoginOtpChallenge.MAX_ATTEMPTS - 1);
        var command = new VerifyLoginOtpCommand(challengeId, "000000");

        when(loginOtpChallengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        Result<VerifyOtpOutcome> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Too many attempts");

        ArgumentCaptor<LoginOtpChallenge> saved = ArgumentCaptor.forClass(LoginOtpChallenge.class);
        verify(loginOtpChallengeRepository).save(saved.capture());
        assertThat(saved.getValue().attempts()).isEqualTo(LoginOtpChallenge.MAX_ATTEMPTS);
        assertThat(saved.getValue().isLocked()).isTrue();
    }

    @Test
    void should_reject_locked_challenge_even_with_correct_code() {
        UUID challengeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var challenge = pendingChallenge(challengeId, userId, "123456", LoginOtpChallenge.MAX_ATTEMPTS);
        var command = new VerifyLoginOtpCommand(challengeId, "123456");

        when(loginOtpChallengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        Result<VerifyOtpOutcome> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Too many attempts");
        verify(loginOtpChallengeRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
        verify(trustedDeviceRepository, never()).save(any());
    }

    @Test
    void should_fail_when_otp_code_is_expired() {
        UUID challengeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var challenge = new LoginOtpChallenge(
                challengeId,
                userId,
                otpHasher.hash("123456"),
                Instant.now().minus(Duration.ofMinutes(1)),
                false,
                Instant.now().minus(Duration.ofMinutes(10)),
                null,
                0
        );
        var command = new VerifyLoginOtpCommand(challengeId, "123456");

        when(loginOtpChallengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        Result<VerifyOtpOutcome> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("OTP code expired");
    }
}
