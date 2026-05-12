package com.cyna.modules.user.application.command.logout;

import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutCommandHandlerTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    private LogoutCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new LogoutCommandHandler(refreshTokenRepository, transactionRunner);
    }

    @Test
    void should_revoke_only_current_session_when_allDevices_is_false() {
        var userId = UUID.randomUUID();
        var stored = new RefreshToken(
                UUID.randomUUID(), userId, "hashed-token",
                Instant.now().plus(7, ChronoUnit.DAYS), false, Instant.now()
        );

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        Result<Void> result = handler.handle(new LogoutCommand("raw-token", false));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().revoked()).isTrue();
        assertThat(captor.getValue().id()).isEqualTo(stored.id());
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void should_revoke_all_sessions_when_allDevices_is_true() {
        var userId = UUID.randomUUID();
        var stored = new RefreshToken(
                UUID.randomUUID(), userId, "hashed-token",
                Instant.now().plus(7, ChronoUnit.DAYS), false, Instant.now()
        );

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        Result<Void> result = handler.handle(new LogoutCommand("raw-token", true));

        assertThat(result.isSuccess()).isTrue();
        verify(refreshTokenRepository).revokeAllByUserId(userId);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void should_fail_when_token_not_found() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        Result<Void> result = handler.handle(new LogoutCommand("unknown-token", false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid refresh token");
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void should_default_allDevices_to_false_via_legacy_constructor() {
        var command = new LogoutCommand("raw-token");
        assertThat(command.allDevices()).isFalse();
    }
}
