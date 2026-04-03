package com.cyna.modules.user.application.command.login;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginCommandHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    private LoginCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new LoginCommandHandler(
                userRepository, passwordHasher, jwtProvider,
                refreshTokenRepository, transactionRunner
        );
    }

    @Test
    void should_login_successfully() {
        var command = new LoginCommand("test@example.com", "password123");
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("hashed"), "John", "Doe", "fr");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", user.getHashedPassword())).thenReturn(true);
        when(jwtProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtProvider.getAccessTokenExpirationHours()).thenReturn(1L);
        when(jwtProvider.getRefreshTokenExpirationHours()).thenReturn(24L);

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().accessToken()).isEqualTo("access-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void should_fail_when_email_not_found() {
        var command = new LoginCommand("unknown@example.com", "password123");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid credentials");
    }

    @Test
    void should_fail_when_password_does_not_match() {
        var command = new LoginCommand("test@example.com", "wrongpassword");
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("hashed"), "John", "Doe", "fr");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrongpassword", user.getHashedPassword())).thenReturn(false);

        Result<AuthTokens> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid credentials");
    }
}
