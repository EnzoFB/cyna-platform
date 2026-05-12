package com.cyna.modules.user.application.command.passwordreset;

import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.event.PasswordResetRequested;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.PasswordResetToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.repository.PasswordResetTokenRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Starts the forgot-password flow.
 *
 * <p>Always reports success to the caller, independent of whether the email is
 * actually registered — leaking that information would enable address-
 * enumeration. When the email matches a real user, any previous pending reset
 * tokens are deleted (so the mailbox never accumulates more than one valid
 * link) and a fresh token is created. The {@code PasswordResetRequested} event
 * carries the raw token to the listener that sends the email.</p>
 */
@Component
public class RequestPasswordResetCommandHandler
        implements CommandHandler<RequestPasswordResetCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(RequestPasswordResetCommandHandler.class);
    private static final String DEFAULT_LANG = "fr";
    private static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JwtProvider jwtProvider;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public RequestPasswordResetCommandHandler(UserRepository userRepository,
                                              PasswordResetTokenRepository tokenRepository,
                                              JwtProvider jwtProvider,
                                              DomainEventPublisher eventPublisher,
                                              TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.jwtProvider = jwtProvider;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(RequestPasswordResetCommand command) {
        String lang = (command.lang() == null || command.lang().isBlank()) ? DEFAULT_LANG : command.lang();

        Email email;
        try {
            email = Email.of(command.email());
        } catch (Exception invalid) {
            // Invalid format — silently succeed (same response as unknown email).
            return Result.success();
        }

        var optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            log.info("[password-reset-request] no user for email — silently succeeding");
            return Result.success();
        }

        var user = optUser.get();
        UUID userId = user.getId();
        String rawToken = jwtProvider.generateRefreshToken();
        String tokenHash = TokenHash.of(rawToken);
        Instant expiresAt = Instant.now().plus(TOKEN_TTL);

        transactionRunner.run(() -> {
            tokenRepository.deleteUnconsumedByUserId(userId);
            tokenRepository.save(PasswordResetToken.create(userId, tokenHash, expiresAt));

            eventPublisher.publish(new PasswordResetRequested(
                    userId,
                    user.getEmail().value(),
                    user.getFirstName(),
                    rawToken,
                    lang,
                    Instant.now()
            ));
        });

        log.info("[password-reset-request] issued reset token userId={}", userId);
        return Result.success();
    }
}
