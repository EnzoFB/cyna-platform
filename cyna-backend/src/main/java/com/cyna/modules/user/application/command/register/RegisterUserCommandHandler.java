package com.cyna.modules.user.application.command.register;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.model.UserConsentAction;
import com.cyna.modules.user.domain.model.UserConsentLog;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.repository.UserConsentLogRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class RegisterUserCommandHandler implements CommandHandler<RegisterUserCommand, AuthTokens> {

    /**
     * Version of the Terms/Privacy wording shown at registration. Stamped
     * server-side so the recorded consent always reflects what we actually
     * presented. Bump this whenever the legal wording changes.
     */
    public static final String TERMS_PRIVACY_VERSION = "2026-05-16";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserConsentLogRepository consentLogRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public RegisterUserCommandHandler(UserRepository userRepository,
                                      PasswordHasher passwordHasher,
                                      JwtProvider jwtProvider,
                                      RefreshTokenRepository refreshTokenRepository,
                                      UserConsentLogRepository consentLogRepository,
                                      DomainEventPublisher eventPublisher,
                                      TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.consentLogRepository = consentLogRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<AuthTokens> handle(RegisterUserCommand command) {
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

            String accessToken = jwtProvider.generateAccessToken(user);
            String rawRefreshToken = jwtProvider.generateRefreshToken();

            RefreshToken refreshToken = RefreshToken.create(
                    user.getId(),
                    TokenHash.of(rawRefreshToken),
                    Instant.now().plus(Duration.ofHours(jwtProvider.getRefreshTokenExpirationHours()))
            );
            refreshTokenRepository.save(refreshToken);

            eventPublisher.publishAll(user.getDomainEvents());
            user.clearDomainEvents();

            return Result.success(new AuthTokens(
                    accessToken,
                    rawRefreshToken,
                    jwtProvider.getAccessTokenExpirationHours()
            ));
        });
    }
}
