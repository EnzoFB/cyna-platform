package com.cyna.modules.user.application.command.register;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserCommandHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private DomainEventPublisher eventPublisher;

    private RegisterUserCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new RegisterUserCommandHandler(
                userRepository, passwordHasher, jwtProvider,
                refreshTokenRepository, eventPublisher, transactionRunner
        );
    }

    @Test
    void should_register_user_successfully() {
        var command = new RegisterUserCommand("test@example.com", "password123", "John", "Doe", "fr");

        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn(HashedPassword.of("hashed"));
        when(jwtProvider.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtProvider.getAccessTokenExpirationHours()).thenReturn(1L);
        when(jwtProvider.getRefreshTokenExpirationHours()).thenReturn(24L);

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().accessToken()).isEqualTo("access-token");
        assertThat(result.getValue().refreshToken()).isEqualTo("refresh-token");

        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(eventPublisher).publishAll(anyList());
    }

    @Test
    void should_fail_when_email_already_exists() {
        var command = new RegisterUserCommand("existing@example.com", "password123", "John", "Doe", "fr");

        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Email already exists");

        verify(userRepository, never()).save(any());
    }
}
