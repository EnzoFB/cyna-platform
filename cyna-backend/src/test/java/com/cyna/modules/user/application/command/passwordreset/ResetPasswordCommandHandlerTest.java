package com.cyna.modules.user.application.command.passwordreset;

import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.event.UserPasswordChanged;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.PasswordResetToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.PasswordResetTokenRepository;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.DomainEvent;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetPasswordCommandHandlerTest {

    private static final String RAW_TOKEN = "raw-token-uuid";
    private static final String TOKEN_HASH = TokenHash.of(RAW_TOKEN);

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private DomainEventPublisher eventPublisher;

    private ResetPasswordCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new ResetPasswordCommandHandler(
                userRepository, tokenRepository, refreshTokenRepository,
                passwordHasher, eventPublisher, transactionRunner
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_reset_password_revoke_sessions_and_consume_token_on_success() {
        var user = User.register(Email.of("victim@example.com"), HashedPassword.of("old-hash"), "Alice", "Doe", "fr");
        var token = PasswordResetToken.create(user.getId(), TOKEN_HASH,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordHasher.hash("new-password")).thenReturn(HashedPassword.of("new-hash"));

        Result<Void> result = handler.handle(new ResetPasswordCommand(RAW_TOKEN, "new-password"));

        assertThat(result.isSuccess()).isTrue();
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());

        ArgumentCaptor<PasswordResetToken> consumedCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(consumedCaptor.capture());
        assertThat(consumedCaptor.getValue().consumed()).isTrue();
        assertThat(consumedCaptor.getValue().id()).isEqualTo(token.id());

        ArgumentCaptor<List<DomainEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publishAll(eventsCaptor.capture());
        assertThat(eventsCaptor.getValue())
                .anyMatch(UserPasswordChanged.class::isInstance);
    }

    @Test
    void should_fail_when_token_is_unknown() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        Result<Void> result = handler.handle(new ResetPasswordCommand(RAW_TOKEN, "new-password"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid or expired reset token");
        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(eventPublisher);
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_fail_when_token_is_expired() {
        var expired = PasswordResetToken.create(UUID.randomUUID(), TOKEN_HASH,
                Instant.now().minus(1, ChronoUnit.HOURS));
        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(expired));

        Result<Void> result = handler.handle(new ResetPasswordCommand(RAW_TOKEN, "new-password"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid or expired reset token");
        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_fail_when_token_already_consumed() {
        var consumed = PasswordResetToken.create(UUID.randomUUID(), TOKEN_HASH,
                Instant.now().plus(30, ChronoUnit.MINUTES)).consume();
        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(consumed));

        Result<Void> result = handler.handle(new ResetPasswordCommand(RAW_TOKEN, "new-password"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid or expired reset token");
        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_fail_when_user_not_found() {
        var orphanToken = PasswordResetToken.create(UUID.randomUUID(), TOKEN_HASH,
                Instant.now().plus(30, ChronoUnit.MINUTES));
        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(orphanToken));
        when(userRepository.findById(orphanToken.userId())).thenReturn(Optional.empty());

        Result<Void> result = handler.handle(new ResetPasswordCommand(RAW_TOKEN, "new-password"));

        assertThat(result.isFailure()).isTrue();
        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(eventPublisher);
    }
}
