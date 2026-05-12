package com.cyna.modules.user.application.command.password;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
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
import java.util.List;

/**
 * Changes a user's password and rotates the entire session footprint.
 *
 * <p>All existing refresh tokens are revoked in the same transaction as the
 * password update — a leaked refresh token cannot survive a password change.
 * A fresh pair of tokens is issued so the device that just changed the
 * password stays logged in. A {@code UserPasswordChanged} event is published
 * after commit; a security alert email is sent to the registered address.</p>
 */
@Component
public class ChangePasswordCommandHandler implements CommandHandler<ChangePasswordCommand, AuthTokens> {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordCommandHandler.class);
    private static final String DEFAULT_LANG = "fr";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final JwtProvider jwtProvider;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public ChangePasswordCommandHandler(UserRepository userRepository,
                                        RefreshTokenRepository refreshTokenRepository,
                                        PasswordHasher passwordHasher,
                                        JwtProvider jwtProvider,
                                        DomainEventPublisher eventPublisher,
                                        TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.jwtProvider = jwtProvider;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<AuthTokens> handle(ChangePasswordCommand command) {
        var optUser = userRepository.findById(command.userId());
        if (optUser.isEmpty()) {
            return Result.failure("User not found");
        }

        var user = optUser.get();

        if (!passwordHasher.matches(command.currentPassword(), user.getHashedPassword())) {
            return Result.failure("Current password is incorrect");
        }

        var newHashed = passwordHasher.hash(command.newPassword());
        var updated = user.changePassword(newHashed, DEFAULT_LANG);

        return transactionRunner.runReturning(() -> {
            userRepository.save(updated);
            refreshTokenRepository.revokeAllByUserId(user.getId());

            String newAccessToken = jwtProvider.generateAccessToken(updated);
            String newRawRefreshToken = jwtProvider.generateRefreshToken();

            RefreshToken newRefreshToken = RefreshToken.create(
                    user.getId(),
                    TokenHash.of(newRawRefreshToken),
                    Instant.now().plus(Duration.ofHours(jwtProvider.getRefreshTokenExpirationHours()))
            );
            refreshTokenRepository.save(newRefreshToken);

            // Defensive copy: getDomainEvents() returns a live view; clearing
            // the aggregate would empty it under the publisher's feet.
            eventPublisher.publishAll(List.copyOf(updated.getDomainEvents()));
            updated.clearDomainEvents();

            log.info("[change-password] revoked all sessions and rotated tokens userId={}", user.getId());

            return Result.success(new AuthTokens(
                    newAccessToken,
                    newRawRefreshToken,
                    jwtProvider.getAccessTokenExpirationHours()
            ));
        });
    }
}
