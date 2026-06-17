package com.cyna.modules.user.application.command.register;

import com.cyna.modules.user.application.model.RegistrationResult;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.event.EmailVerificationRequested;
import com.cyna.modules.user.domain.model.EmailVerificationToken;
import com.cyna.modules.user.domain.model.UserStatus;
import com.cyna.modules.user.domain.repository.EmailVerificationTokenRepository;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.UserConsentLogRepository;
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

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserCommandHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private JwtProvider jwtProvider;
    @Mock private EmailVerificationTokenRepository verificationTokenRepository;
    @Mock private UserConsentLogRepository consentLogRepository;
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
                verificationTokenRepository, consentLogRepository, eventPublisher, transactionRunner
        );
    }

    @Test
    void should_register_user_as_pending_and_issue_verification_token() {
        var command = new RegisterUserCommand("test@example.com", "password123", "John", "Doe", "Acme", "fr",
                true, "127.0.0.1", "JUnit");

        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn(HashedPassword.of("hashed"));
        when(jwtProvider.generateRefreshToken()).thenReturn("raw-verification-token");

        Result<RegistrationResult> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        // No JWT is issued at registration — only a PENDING status + the email.
        assertThat(result.getValue().status()).isEqualTo(UserStatus.PENDING_VERIFICATION.name());
        assertThat(result.getValue().email()).isEqualTo("test@example.com");

        // The persisted user is PENDING_VERIFICATION.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);

        // A single-use verification token is stored (hashed, never the raw value).
        verify(verificationTokenRepository).save(any(EmailVerificationToken.class));
        // RGPD Art. 7.1 — registration must record the terms/privacy consent.
        verify(consentLogRepository).save(any());

        // The EmailVerificationRequested event carries the raw token to the mailer.
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(EmailVerificationRequested.class);
        assertThat(((EmailVerificationRequested) eventCaptor.getValue()).rawToken())
                .isEqualTo("raw-verification-token");
    }

    @Test
    void should_fail_when_email_already_exists() {
        var command = new RegisterUserCommand("existing@example.com", "password123", "John", "Doe", "Acme", "fr",
                true, "127.0.0.1", "JUnit");

        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        Result<RegistrationResult> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Email already exists");

        verify(userRepository, never()).save(any());
    }
}
