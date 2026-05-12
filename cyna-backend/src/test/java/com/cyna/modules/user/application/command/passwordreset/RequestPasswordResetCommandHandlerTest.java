package com.cyna.modules.user.application.command.passwordreset;

import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.event.PasswordResetRequested;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.PasswordResetToken;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.PasswordResetTokenRepository;
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

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestPasswordResetCommandHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private DomainEventPublisher eventPublisher;

    private RequestPasswordResetCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new RequestPasswordResetCommandHandler(
                userRepository, tokenRepository, jwtProvider, eventPublisher, transactionRunner
        );
    }

    @Test
    void should_issue_token_and_publish_event_when_user_exists() {
        var user = User.register(Email.of("victim@example.com"), HashedPassword.of("hash"), "Alice", "Doe", "fr");

        when(userRepository.findByEmail(Email.of("victim@example.com"))).thenReturn(Optional.of(user));
        when(jwtProvider.generateRefreshToken()).thenReturn("raw-token-uuid");

        Result<Void> result = handler.handle(new RequestPasswordResetCommand("victim@example.com", "fr"));

        assertThat(result.isSuccess()).isTrue();
        verify(tokenRepository).deleteUnconsumedByUserId(user.getId());
        verify(tokenRepository).save(any(PasswordResetToken.class));

        ArgumentCaptor<PasswordResetRequested> captor = ArgumentCaptor.forClass(PasswordResetRequested.class);
        verify(eventPublisher).publish(captor.capture());
        var event = captor.getValue();
        assertThat(event.userId()).isEqualTo(user.getId());
        assertThat(event.email()).isEqualTo("victim@example.com");
        assertThat(event.firstName()).isEqualTo("Alice");
        assertThat(event.rawToken()).isEqualTo("raw-token-uuid");
        assertThat(event.lang()).isEqualTo("fr");
    }

    @Test
    void should_silently_succeed_when_email_is_unknown_to_prevent_enumeration() {
        when(userRepository.findByEmail(Email.of("ghost@example.com"))).thenReturn(Optional.empty());

        Result<Void> result = handler.handle(new RequestPasswordResetCommand("ghost@example.com", "fr"));

        assertThat(result.isSuccess()).isTrue();
        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(eventPublisher);
        verify(jwtProvider, never()).generateRefreshToken();
    }

    @Test
    void should_silently_succeed_when_email_format_is_invalid() {
        Result<Void> result = handler.handle(new RequestPasswordResetCommand("not-an-email", "fr"));

        assertThat(result.isSuccess()).isTrue();
        verifyNoInteractions(userRepository);
        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_default_lang_to_fr_when_blank() {
        var user = User.register(Email.of("u@example.com"), HashedPassword.of("hash"), "Bob", "Doe", "fr");
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(jwtProvider.generateRefreshToken()).thenReturn("t");

        handler.handle(new RequestPasswordResetCommand("u@example.com", null));

        ArgumentCaptor<PasswordResetRequested> captor = ArgumentCaptor.forClass(PasswordResetRequested.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().lang()).isEqualTo("fr");
    }
}
