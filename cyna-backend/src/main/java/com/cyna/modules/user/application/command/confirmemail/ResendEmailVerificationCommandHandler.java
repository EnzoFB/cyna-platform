package com.cyna.modules.user.application.command.confirmemail;

import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.event.EmailVerificationRequested;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.EmailVerificationToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.UserStatus;
import com.cyna.modules.user.domain.repository.EmailVerificationTokenRepository;
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

/**
 * Re-issues an email-verification link for an account that is still
 * {@code PENDING_VERIFICATION}.
 *
 * <p>Always reports success to the caller, independent of whether the email is
 * registered or the account is already verified — leaking that would enable
 * address enumeration (same posture as forgot-password). When a pending account
 * matches, any previous pending token is deleted (so the mailbox never holds
 * more than one valid link) and a fresh one is created.</p>
 */
@Component
public class ResendEmailVerificationCommandHandler
        implements CommandHandler<ResendEmailVerificationCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailVerificationCommandHandler.class);
    private static final String DEFAULT_LANG = "fr";
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final JwtProvider jwtProvider;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public ResendEmailVerificationCommandHandler(UserRepository userRepository,
                                                 EmailVerificationTokenRepository tokenRepository,
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
    public Result<Void> handle(ResendEmailVerificationCommand command) {
        String lang = (command.lang() == null || command.lang().isBlank()) ? DEFAULT_LANG : command.lang();

        Email email;
        try {
            email = Email.of(command.email());
        } catch (Exception invalid) {
            // Invalid format — silently succeed (same response as unknown email).
            return Result.success();
        }

        var optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty() || optUser.get().getStatus() != UserStatus.PENDING_VERIFICATION) {
            // Unknown email or already-verified/other-state account — silently
            // succeed to avoid enumeration.
            log.info("[resend-confirmation] no pending account for email — silently succeeding");
            return Result.success();
        }

        var user = optUser.get();
        String rawToken = jwtProvider.generateRefreshToken();
        String tokenHash = TokenHash.of(rawToken);
        Instant expiresAt = Instant.now().plus(TOKEN_TTL);

        transactionRunner.run(() -> {
            tokenRepository.deleteUnconsumedByUserId(user.getId());
            tokenRepository.save(EmailVerificationToken.create(user.getId(), tokenHash, expiresAt));

            eventPublisher.publish(new EmailVerificationRequested(
                    user.getId(),
                    user.getEmail().value(),
                    user.getFirstName(),
                    rawToken,
                    lang,
                    Instant.now()
            ));
        });

        log.info("[resend-confirmation] re-issued verification token userId={}", user.getId());
        return Result.success();
    }
}
