package com.cyna.modules.user.application.command.password;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.event.UserPasswordChanged;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.User;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordCommandHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private JwtProvider jwtProvider;
    @Mock private DomainEventPublisher eventPublisher;

    private ChangePasswordCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new ChangePasswordCommandHandler(
                userRepository, refreshTokenRepository, passwordHasher, jwtProvider,
                eventPublisher, transactionRunner
        );
    }

    @Test
    void should_revoke_all_sessions_and_issue_new_tokens_on_success() {
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("old-hash"), "John", "Doe", "fr");
        var command = new ChangePasswordCommand(user.getId(), "old-password", "new-password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("old-password", user.getHashedPassword())).thenReturn(true);
        when(passwordHasher.hash("new-password")).thenReturn(HashedPassword.of("new-hash"));
        when(jwtProvider.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtProvider.generateRefreshToken()).thenReturn("new-refresh-token");
        when(jwtProvider.getAccessTokenExpirationHours()).thenReturn(1L);
        when(jwtProvider.getRefreshTokenExpirationHours()).thenReturn(24L);

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().accessToken()).isEqualTo("new-access-token");
        assertThat(result.getValue().refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.getValue().expiresIn()).isEqualTo(1L);
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishAll(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_publish_user_password_changed_event_on_success() {
        var user = User.register(Email.of("alert@example.com"), HashedPassword.of("old-hash"), "Alice", "Doe", "fr");
        var command = new ChangePasswordCommand(user.getId(), "old-password", "new-password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("old-password", user.getHashedPassword())).thenReturn(true);
        when(passwordHasher.hash("new-password")).thenReturn(HashedPassword.of("new-hash"));
        when(jwtProvider.generateAccessToken(any())).thenReturn("at");
        when(jwtProvider.generateRefreshToken()).thenReturn("rt");
        when(jwtProvider.getRefreshTokenExpirationHours()).thenReturn(24L);

        handler.handle(command);

        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publishAll(captor.capture());
        var event = captor.getValue().stream()
                .filter(UserPasswordChanged.class::isInstance)
                .map(UserPasswordChanged.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(event.email()).isEqualTo("alert@example.com");
        assertThat(event.firstName()).isEqualTo("Alice");
    }

    @Test
    void should_fail_when_current_password_does_not_match() {
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("hash"), "John", "Doe", "fr");
        var command = new ChangePasswordCommand(user.getId(), "wrong-password", "new-password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong-password", user.getHashedPassword())).thenReturn(false);

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Current password is incorrect");
        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(eventPublisher);
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_fail_when_user_not_found() {
        var userId = UUID.randomUUID();
        var command = new ChangePasswordCommand(userId, "any", "new");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("User not found");
        verifyNoInteractions(refreshTokenRepository);
        verifyNoInteractions(eventPublisher);
    }
}
