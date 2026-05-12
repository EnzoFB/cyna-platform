package com.cyna.modules.user.application.command.passwordreset;

import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.PasswordResetToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.repository.PasswordResetTokenRepository;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Finalises the forgot-password flow.
 *
 * <p>Verifies the one-shot token, replaces the user's password, marks the
 * token as consumed and revokes every active refresh token — same posture
 * as a change-password from inside an authenticated session, except no new
 * token pair is issued (the user is unauthenticated and must log back in).
 * {@code UserPasswordChanged} is raised by the aggregate so the existing
 * security-alert listener emails the user automatically.</p>
 */
@Component
public class ResetPasswordCommandHandler implements CommandHandler<ResetPasswordCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(ResetPasswordCommandHandler.class);
    private static final String DEFAULT_LANG = "fr";
    private static final String INVALID_OR_EXPIRED = "Invalid or expired reset token";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public ResetPasswordCommandHandler(UserRepository userRepository,
                                       PasswordResetTokenRepository tokenRepository,
                                       RefreshTokenRepository refreshTokenRepository,
                                       PasswordHasher passwordHasher,
                                       DomainEventPublisher eventPublisher,
                                       TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(ResetPasswordCommand command) {
        String tokenHash = TokenHash.of(command.token());

        Optional<PasswordResetToken> optToken = tokenRepository.findByTokenHash(tokenHash);
        if (optToken.isEmpty()) {
            return Result.failure(INVALID_OR_EXPIRED);
        }

        PasswordResetToken token = optToken.get();
        if (token.consumed() || token.isExpired()) {
            return Result.failure(INVALID_OR_EXPIRED);
        }

        var optUser = userRepository.findById(token.userId());
        if (optUser.isEmpty()) {
            return Result.failure(INVALID_OR_EXPIRED);
        }

        var user = optUser.get();
        var newHashed = passwordHasher.hash(command.newPassword());
        var updated = user.changePassword(newHashed, DEFAULT_LANG);

        transactionRunner.run(() -> {
            userRepository.save(updated);
            tokenRepository.save(token.consume());
            refreshTokenRepository.revokeAllByUserId(user.getId());

            // Defensive copy: getDomainEvents() returns a live view; clearing
            // the aggregate would empty it under the publisher's feet.
            eventPublisher.publishAll(List.copyOf(updated.getDomainEvents()));
            updated.clearDomainEvents();
        });

        log.info("[password-reset] succeeded, revoked all sessions userId={}", user.getId());
        return Result.success();
    }
}
