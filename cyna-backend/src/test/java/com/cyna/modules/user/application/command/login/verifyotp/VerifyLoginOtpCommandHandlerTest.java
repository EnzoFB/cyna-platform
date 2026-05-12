package com.cyna.modules.user.application.command.login.verifyotp;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.LoginOtpChallenge;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.LoginOtpChallengeRepository;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyLoginOtpCommandHandlerTest {

    @Mock private LoginOtpChallengeRepository loginOtpChallengeRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtProvider jwtProvider;

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
                jwtProvider,
                transactionRunner
        );
    }

    @Test
    void should_verify_otp_and_return_tokens() {
        UUID challengeId = UUID.randomUUID();
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("hashed"), "John", "Doe", "fr");
        var challenge = new LoginOtpChallenge(
                challengeId,
                user.getId(),
                TokenHash.of("123456"),
                Instant.now().plus(Duration.ofMinutes(5)),
                false,
                Instant.now(),
                null
        );
        var command = new VerifyLoginOtpCommand(challengeId, "123456");

        when(loginOtpChallengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtProvider.getAccessTokenExpirationHours()).thenReturn(1L);
        when(jwtProvider.getRefreshTokenExpirationHours()).thenReturn(24L);

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().accessToken()).isEqualTo("access-token");
        assertThat(result.getValue().refreshToken()).isEqualTo("refresh-token");
        verify(loginOtpChallengeRepository).save(any(LoginOtpChallenge.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void should_fail_when_challenge_not_found() {
        UUID challengeId = UUID.randomUUID();
        var command = new VerifyLoginOtpCommand(challengeId, "123456");

        when(loginOtpChallengeRepository.findById(challengeId)).thenReturn(Optional.empty());

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid OTP challenge");
    }

    @Test
    void should_fail_when_otp_code_is_invalid() {
        UUID challengeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var challenge = new LoginOtpChallenge(
                challengeId,
                userId,
                TokenHash.of("123456"),
                Instant.now().plus(Duration.ofMinutes(5)),
                false,
                Instant.now(),
                null
        );
        var command = new VerifyLoginOtpCommand(challengeId, "654321");

        when(loginOtpChallengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid OTP code");
    }

    @Test
    void should_fail_when_otp_code_is_expired() {
        UUID challengeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var challenge = new LoginOtpChallenge(
                challengeId,
                userId,
                TokenHash.of("123456"),
                Instant.now().minus(Duration.ofMinutes(1)),
                false,
                Instant.now().minus(Duration.ofMinutes(10)),
                null
        );
        var command = new VerifyLoginOtpCommand(challengeId, "123456");

        when(loginOtpChallengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("OTP code expired");
    }
}
