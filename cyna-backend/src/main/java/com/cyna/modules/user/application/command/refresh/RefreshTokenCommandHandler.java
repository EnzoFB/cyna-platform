package com.cyna.modules.user.application.command.refresh;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.event.SuspiciousAuthActivityDetected;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.User;
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
import java.util.Optional;

@Component
public class RefreshTokenCommandHandler implements CommandHandler<RefreshTokenCommand, AuthTokens> {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCommandHandler.class);
    private static final String DEFAULT_LANG = "fr";

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public RefreshTokenCommandHandler(RefreshTokenRepository refreshTokenRepository,
                                      UserRepository userRepository,
                                      JwtProvider jwtProvider,
                                      DomainEventPublisher eventPublisher,
                                      TransactionRunner transactionRunner) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<AuthTokens> handle(RefreshTokenCommand command) {
        return transactionRunner.runReturning(() -> {
            String tokenHash = TokenHash.of(command.refreshToken());

            // PESSIMISTIC_WRITE: serialises concurrent rotations of the same
            // token, so the check-then-update race cannot produce two valid
            // rotated pairs from a single original token.
            Optional<RefreshToken> storedOpt = refreshTokenRepository.findByTokenHashForUpdate(tokenHash);
            if (storedOpt.isEmpty()) {
                return Result.<AuthTokens>failure("Invalid refresh token");
            }

            RefreshToken stored = storedOpt.get();

            // Reuse detection: a revoked token being presented means it was
            // stolen and replayed (or a buggy client). Scorched-earth — revoke
            // every active session and alert the user by email.
            if (stored.revoked()) {
                refreshTokenRepository.revokeAllByUserId(stored.userId());
                userRepository.findById(stored.userId()).ifPresent(user ->
                        eventPublisher.publish(new SuspiciousAuthActivityDetected(
                                user.getId(),
                                user.getEmail().value(),
                                user.getFirstName(),
                                DEFAULT_LANG,
                                SuspiciousAuthActivityDetected.Reason.REFRESH_TOKEN_REUSE,
                                Instant.now()
                        ))
                );
                log.warn("[refresh] reuse detected, revoked all sessions userId={}", stored.userId());
                return Result.<AuthTokens>failure("Refresh token reuse detected");
            }

            if (stored.isExpired()) {
                return Result.<AuthTokens>failure("Refresh token expired");
            }

            Optional<User> userOpt = userRepository.findById(stored.userId());
            if (userOpt.isEmpty()) {
                return Result.<AuthTokens>failure("User not found");
            }

            User user = userOpt.get();

            RefreshToken revoked = new RefreshToken(
                    stored.id(), stored.userId(), stored.tokenHash(),
                    stored.expiresAt(), true, stored.createdAt()
            );
            refreshTokenRepository.save(revoked);

            String newAccessToken = jwtProvider.generateAccessToken(user);
            String newRawRefreshToken = jwtProvider.generateRefreshToken();

            RefreshToken newRefreshToken = RefreshToken.create(
                    user.getId(),
                    TokenHash.of(newRawRefreshToken),
                    Instant.now().plus(Duration.ofHours(jwtProvider.getRefreshTokenExpirationHours()))
            );
            refreshTokenRepository.save(newRefreshToken);

            return Result.<AuthTokens>success(new AuthTokens(
                    newAccessToken,
                    newRawRefreshToken,
                    jwtProvider.getAccessTokenExpirationHours()
            ));
        });
    }
}
