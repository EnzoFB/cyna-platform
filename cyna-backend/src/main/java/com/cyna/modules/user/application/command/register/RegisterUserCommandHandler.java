package com.cyna.modules.user.application.command.register;

import com.cyna.modules.user.application.model.RegistrationResult;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.event.EmailVerificationRequested;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.EmailVerificationToken;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.model.UserConsentAction;
import com.cyna.modules.user.domain.model.UserConsentLog;
import com.cyna.modules.user.domain.model.UserStatus;
import com.cyna.modules.user.domain.repository.EmailVerificationTokenRepository;
import com.cyna.modules.user.domain.repository.UserConsentLogRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Self-service registration.
 *
 * <p>The account is created in {@code PENDING_VERIFICATION} — no JWT is issued
 * and no welcome email is sent here. A single-use email-verification token
 * (hashed, valid 24h) is generated and the raw value is carried on the
 * {@link EmailVerificationRequested} event to the listener that mails the
 * confirmation link. The user becomes ACTIVE (and is auto-logged-in) only after
 * clicking that link — see {@code ConfirmEmailCommandHandler}.</p>
 */
@Component
public class RegisterUserCommandHandler implements CommandHandler<RegisterUserCommand, RegistrationResult> {

    /**
     * Version of the Terms/Privacy wording shown at registration. Stamped
     * server-side so the recorded consent always reflects what we actually
     * presented. Bump this whenever the legal wording changes.
     */
    public static final String TERMS_PRIVACY_VERSION = "2026-05-16";

    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtProvider jwtProvider;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final UserConsentLogRepository consentLogRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public RegisterUserCommandHandler(UserRepository userRepository,
                                      PasswordHasher passwordHasher,
                                      JwtProvider jwtProvider,
                                      EmailVerificationTokenRepository verificationTokenRepository,
                                      UserConsentLogRepository consentLogRepository,
                                      DomainEventPublisher eventPublisher,
                                      TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtProvider = jwtProvider;
        this.verificationTokenRepository = verificationTokenRepository;
        this.consentLogRepository = consentLogRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<RegistrationResult> handle(RegisterUserCommand command) {
        var email = Email.of(command.email());

        // RGPD Art. 7 — consent must be explicit. The DTO @AssertTrue already
        // rejects a missing tick at the edge; this is defense in depth so the
        // command can never create an account without a recorded consent.
        if (!command.acceptedTerms()) {
            return Result.failure("Terms of Service and Privacy Policy must be accepted");
        }

        if (userRepository.existsByEmail(email)) {
            return Result.failure("Email already exists");
        }

        return transactionRunner.runReturning(() -> {
            HashedPassword hashedPassword = passwordHasher.hash(command.password());

            User user = User.register(email, hashedPassword, command.firstName(), command.lastName(),
                    command.company(), command.lang());
            userRepository.save(user);

            // Append-only proof that this user accepted the Terms/Privacy at
            // registration, which wording version, when and from where.
            consentLogRepository.save(UserConsentLog.record(
                    user.getId(),
                    UserConsentAction.TERMS_AND_PRIVACY,
                    TERMS_PRIVACY_VERSION,
                    command.ipAddress(),
                    command.userAgent()
            ));

            // Single-use email-verification token (hashed at rest, raw value
            // only ever lives in the event/email). UUID source reused from the
            // refresh-token generator, same as the password-reset flow.
            String rawToken = jwtProvider.generateRefreshToken();
            verificationTokenRepository.save(EmailVerificationToken.create(
                    user.getId(),
                    TokenHash.of(rawToken),
                    Instant.now().plus(VERIFICATION_TOKEN_TTL)
            ));

            eventPublisher.publish(new EmailVerificationRequested(
                    user.getId(),
                    user.getEmail().value(),
                    user.getFirstName(),
                    rawToken,
                    command.lang(),
                    Instant.now()
            ));

            return Result.success(new RegistrationResult(
                    UserStatus.PENDING_VERIFICATION.name(),
                    user.getEmail().value()
            ));
        });
    }
}
