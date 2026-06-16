package com.cyna.modules.user.application.command.emailchange;

import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.repository.EmailChangeTokenRepository;
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

/**
 * Confirms an email change and revokes all existing refresh tokens.
 *
 * <p>Email is an authentication factor (OTP delivery, password reset). Changing
 * it must invalidate every active session — the user is forced to re-login
 * with the new email. A {@code UserEmailChanged} event is published after
 * commit; a security alert email is sent to the previous address so the
 * original owner can react if the account was hijacked.</p>
 */
@Component
public class ConfirmEmailChangeCommandHandler implements CommandHandler<ConfirmEmailChangeCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(ConfirmEmailChangeCommandHandler.class);
    private static final String DEFAULT_LANG = "fr";

    private final EmailChangeTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public ConfirmEmailChangeCommandHandler(EmailChangeTokenRepository tokenRepository,
                                            UserRepository userRepository,
                                            RefreshTokenRepository refreshTokenRepository,
                                            DomainEventPublisher eventPublisher,
                                            TransactionRunner transactionRunner) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(ConfirmEmailChangeCommand command) {
        var optToken = tokenRepository.findByToken(command.token());
        if (optToken.isEmpty()) {
            return Result.failure("Invalid or unknown token");
        }

        var changeToken = optToken.get();

        if (changeToken.isExpired()) {
            tokenRepository.deleteById(changeToken.id());
            return Result.failure("Token has expired");
        }

        var optUser = userRepository.findById(changeToken.userId());
        if (optUser.isEmpty()) {
            return Result.failure("User not found");
        }

        var updatedUser = optUser.get().withEmail(Email.of(changeToken.newEmail()), DEFAULT_LANG);

        transactionRunner.run(() -> {
            userRepository.save(updatedUser);
            tokenRepository.deleteById(changeToken.id());
            refreshTokenRepository.revokeAllByUserId(updatedUser.getId());

            // Defensive copy: getDomainEvents() returns a live view; clearing
            // the aggregate would empty it under the publisher's feet.
            eventPublisher.publishAll(List.copyOf(updatedUser.getDomainEvents()));
            updatedUser.clearDomainEvents();
        });

        log.info("[email-change] revoked all sessions userId={}", updatedUser.getId());

        return Result.success();
    }
}
