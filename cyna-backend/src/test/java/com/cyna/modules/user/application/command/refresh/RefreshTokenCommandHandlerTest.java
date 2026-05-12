package com.cyna.modules.user.application.command.refresh;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.event.SuspiciousAuthActivityDetected;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCommandHandlerTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private DomainEventPublisher eventPublisher;

    private RefreshTokenCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new RefreshTokenCommandHandler(
                refreshTokenRepository, userRepository, jwtProvider, eventPublisher, transactionRunner
        );
    }

    @Test
    void should_refresh_tokens_successfully() {
        var command = new RefreshTokenCommand("valid-refresh-token");
        var userId = UUID.randomUUID();
        var stored = new RefreshToken(
                UUID.randomUUID(), userId, "hashed-token",
                Instant.now().plus(7, ChronoUnit.DAYS), false, Instant.now()
        );
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("hash"), "John", "Doe", "fr");

        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtProvider.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtProvider.generateRefreshToken()).thenReturn("new-refresh-token");
        when(jwtProvider.getAccessTokenExpirationHours()).thenReturn(1L);
        when(jwtProvider.getRefreshTokenExpirationHours()).thenReturn(24L);

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().accessToken()).isEqualTo("new-access-token");
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_fail_when_token_not_found() {
        var command = new RefreshTokenCommand("unknown-token");

        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid refresh token");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_revoke_all_tokens_and_publish_suspicious_event_when_reuse_detected() {
        var command = new RefreshTokenCommand("reused-token");
        var userId = UUID.randomUUID();
        var stored = new RefreshToken(
                UUID.randomUUID(), userId, "hashed-token",
                Instant.now().plus(7, ChronoUnit.DAYS), true, Instant.now()
        );
        var user = User.register(Email.of("victim@example.com"), HashedPassword.of("hash"), "Bob", "Doe", "fr");

        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Refresh token reuse detected");
        verify(refreshTokenRepository).revokeAllByUserId(userId);

        ArgumentCaptor<SuspiciousAuthActivityDetected> captor =
                ArgumentCaptor.forClass(SuspiciousAuthActivityDetected.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().reason())
                .isEqualTo(SuspiciousAuthActivityDetected.Reason.REFRESH_TOKEN_REUSE);
        assertThat(captor.getValue().email()).isEqualTo("victim@example.com");
        assertThat(captor.getValue().firstName()).isEqualTo("Bob");
    }

    @Test
    void should_fail_when_token_expired() {
        var command = new RefreshTokenCommand("expired-token");
        var stored = new RefreshToken(
                UUID.randomUUID(), UUID.randomUUID(), "hashed-token",
                Instant.now().minus(1, ChronoUnit.DAYS), false, Instant.now()
        );

        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(stored));

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Refresh token expired");
        verifyNoInteractions(eventPublisher);
    }
}
