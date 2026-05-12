package com.cyna.modules.user.application.command.emailchange;

import com.cyna.modules.user.domain.event.UserEmailChanged;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.EmailChangeToken;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.EmailChangeTokenRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmEmailChangeCommandHandlerTest {

    @Mock private EmailChangeTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private DomainEventPublisher eventPublisher;

    private ConfirmEmailChangeCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(java.util.function.Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new ConfirmEmailChangeCommandHandler(
                tokenRepository, userRepository, refreshTokenRepository, eventPublisher, transactionRunner
        );
    }

    @Test
    void should_change_email_and_revoke_all_sessions_on_success() {
        var user = User.register(Email.of("old@example.com"), HashedPassword.of("hash"), "John", "Doe", "fr");
        var changeToken = EmailChangeToken.create(
                user.getId(), "new@example.com", "valid-token",
                Instant.now().plus(1, ChronoUnit.HOURS)
        );

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(changeToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Result<Void> result = handler.handle(new ConfirmEmailChangeCommand("valid-token"));

        assertThat(result.isSuccess()).isTrue();
        verify(userRepository).save(any(User.class));
        verify(tokenRepository).deleteById(changeToken.id());
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_publish_user_email_changed_event_with_both_addresses() {
        var user = User.register(Email.of("old@example.com"), HashedPassword.of("hash"), "Alice", "Doe", "fr");
        var changeToken = EmailChangeToken.create(
                user.getId(), "new@example.com", "valid-token",
                Instant.now().plus(1, ChronoUnit.HOURS)
        );

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(changeToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        handler.handle(new ConfirmEmailChangeCommand("valid-token"));

        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publishAll(captor.capture());
        var event = captor.getValue().stream()
                .filter(UserEmailChanged.class::isInstance)
                .map(UserEmailChanged.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(event.oldEmail()).isEqualTo("old@example.com");
        assertThat(event.newEmail()).isEqualTo("new@example.com");
        assertThat(event.firstName()).isEqualTo("Alice");
    }

    @Test
    void should_fail_when_token_is_unknown() {
        when(tokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        Result<Void> result = handler.handle(new ConfirmEmailChangeCommand("unknown"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid or unknown token");
        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(eventPublisher);
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_fail_and_delete_token_when_expired() {
        var expiredToken = EmailChangeToken.create(
                java.util.UUID.randomUUID(), "new@example.com", "expired-token",
                Instant.now().minus(1, ChronoUnit.HOURS)
        );

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        Result<Void> result = handler.handle(new ConfirmEmailChangeCommand("expired-token"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Token has expired");
        verify(tokenRepository).deleteById(expiredToken.id());
        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(eventPublisher);
    }
}
