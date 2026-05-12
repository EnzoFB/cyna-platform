package com.cyna.modules.user.application.command.login;

import com.cyna.modules.user.application.model.LoginChallenge;
import com.cyna.modules.user.application.port.OtpCodeGenerator;
import com.cyna.modules.user.application.port.OtpDeliveryPort;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.LoginOtpChallenge;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.LoginOtpChallengeRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginCommandHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private OtpCodeGenerator otpCodeGenerator;
    @Mock private OtpDeliveryPort otpDeliveryPort;
    @Mock private LoginOtpChallengeRepository loginOtpChallengeRepository;

    private LoginCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new LoginCommandHandler(
                userRepository,
                passwordHasher,
                otpCodeGenerator,
                otpDeliveryPort,
                loginOtpChallengeRepository,
                transactionRunner,
                6,
                5
        );
    }

    @Test
    void should_create_login_challenge_successfully() {
        var command = new LoginCommand("test@example.com", "password123");
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("hashed"), "John", "Doe", "fr");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", user.getHashedPassword())).thenReturn(true);
        when(otpCodeGenerator.generateNumericCode(6)).thenReturn("123456");

        Result<LoginChallenge> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().challengeId()).isNotNull();
        assertThat(result.getValue().expiresInSeconds()).isEqualTo(300L);
        verify(loginOtpChallengeRepository).save(any(LoginOtpChallenge.class));
        // sendLoginOtp now takes the user's preferred lang as its 4th arg so the
        // mail template can be rendered in the right language. The default
        // LoginCommand(email, password) overload sets lang="fr" — assert on that.
        verify(otpDeliveryPort).sendLoginOtp(eq("test@example.com"), eq("123456"), any(), eq("fr"));
    }

    @Test
    void should_invalidate_previous_active_challenges_before_creating_new_one() {
        var command = new LoginCommand("test@example.com", "password123");
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("hashed"), "John", "Doe", "fr");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", user.getHashedPassword())).thenReturn(true);
        when(otpCodeGenerator.generateNumericCode(6)).thenReturn("123456");

        handler.handle(command);

        // Ordering matters: the delete MUST run before the save, otherwise the
        // newly-created challenge would be wiped out by the same call.
        var order = inOrder(loginOtpChallengeRepository);
        order.verify(loginOtpChallengeRepository).deleteUnconsumedByUserId(user.getId());
        order.verify(loginOtpChallengeRepository).save(any(LoginOtpChallenge.class));
    }

    @Test
    void should_fail_when_email_not_found() {
        var command = new LoginCommand("unknown@example.com", "password123");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        Result<LoginChallenge> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid credentials");
    }

    @Test
    void should_fail_when_password_does_not_match() {
        var command = new LoginCommand("test@example.com", "wrongpassword");
        var user = User.register(Email.of("test@example.com"), HashedPassword.of("hashed"), "John", "Doe", "fr");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrongpassword", user.getHashedPassword())).thenReturn(false);

        Result<LoginChallenge> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid credentials");
    }
}
