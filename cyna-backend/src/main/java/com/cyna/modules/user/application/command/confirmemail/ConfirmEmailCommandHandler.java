package com.cyna.modules.user.application.command.confirmemail;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.EmailVerificationToken;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.EmailVerificationTokenRepository;
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
import java.util.Optional;

/**
 * Finalises email verification.
 *
 * <p>Validates the one-shot token (hash match + not expired + not consumed),
 * activates the account (PENDING_VERIFICATION → ACTIVE), consumes the token and
 * issues a fresh access/refresh token pair so the user is auto-logged-in — the
 * same posture as a successful login. {@code UserEmailVerified} is raised by the
 * aggregate so the welcome email is sent on first activation only.</p>
 */
@Component
public class ConfirmEmailCommandHandler implements CommandHandler<ConfirmEmailCommand, AuthTokens> {

    private static final Logger log = LoggerFactory.getLogger(ConfirmEmailCommandHandler.class);
    private static final String DEFAULT_LANG = "fr";
    public static final String INVALID_OR_EXPIRED_TOKEN = "Invalid or expired verification token";

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public ConfirmEmailCommandHandler(UserRepository userRepository,
                                      EmailVerificationTokenRepository tokenRepository,
                                      RefreshTokenRepository refreshTokenRepository,
                                      JwtProvider jwtProvider,
                                      DomainEventPublisher eventPublisher,
                                      TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProvider = jwtProvider;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<AuthTokens> handle(ConfirmEmailCommand command) {
        String tokenHash = TokenHash.of(command.token());

        Optional<EmailVerificationToken> optToken = tokenRepository.findByTokenHash(tokenHash);
        if (optToken.isEmpty()) {
            return Result.failure(INVALID_OR_EXPIRED_TOKEN);
        }

        EmailVerificationToken token = optToken.get();
        if (token.consumed() || token.isExpired()) {
            return Result.failure(INVALID_OR_EXPIRED_TOKEN);
        }

        var optUser = userRepository.findById(token.userId());
        if (optUser.isEmpty()) {
            return Result.failure(INVALID_OR_EXPIRED_TOKEN);
        }

        Result<User> activation = optUser.get().activate(DEFAULT_LANG);
        if (activation.isFailure()) {
            // Account is no longer pending (already active, deactivated or
            // anonymized) — treat the late link as invalid.
            return Result.failure(INVALID_OR_EXPIRED_TOKEN);
        }
        User activated = activation.getValue();

        return transactionRunner.runReturning(() -> {
            userRepository.save(activated);
            tokenRepository.save(token.consume());

            String accessToken = jwtProvider.generateAccessToken(activated);
            String rawRefreshToken = jwtProvider.generateRefreshToken();
            refreshTokenRepository.save(RefreshToken.create(
                    activated.getId(),
                    TokenHash.of(rawRefreshToken),
                    Instant.now().plus(Duration.ofHours(jwtProvider.getRefreshTokenExpirationHours()))
            ));

            // Defensive copy: getDomainEvents() returns a live view; clearing
            // the aggregate would empty it under the publisher's feet.
            eventPublisher.publishAll(List.copyOf(activated.getDomainEvents()));
            activated.clearDomainEvents();

            log.info("[confirm-email] account activated userId={}", activated.getId());
            return Result.success(new AuthTokens(
                    accessToken,
                    rawRefreshToken,
                    jwtProvider.getAccessTokenExpirationHours()
            ));
        });
    }
}
