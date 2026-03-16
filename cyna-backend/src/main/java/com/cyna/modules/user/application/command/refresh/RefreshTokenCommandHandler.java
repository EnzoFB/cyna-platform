package com.cyna.modules.user.application.command.refresh;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class RefreshTokenCommandHandler implements CommandHandler<RefreshTokenCommand, AuthTokens> {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final TransactionRunner transactionRunner;

    public RefreshTokenCommandHandler(RefreshTokenRepository refreshTokenRepository,
                                      UserRepository userRepository,
                                      JwtProvider jwtProvider,
                                      TransactionRunner transactionRunner) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<AuthTokens> handle(RefreshTokenCommand command) {
        String tokenHash = TokenHash.of(command.refreshToken());

        Optional<RefreshToken> storedOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        if (storedOpt.isEmpty()) {
            return Result.failure("Invalid refresh token");
        }

        RefreshToken stored = storedOpt.get();

        if (stored.revoked()) {
            refreshTokenRepository.revokeAllByUserId(stored.userId());
            return Result.failure("Refresh token reuse detected");
        }

        if (stored.isExpired()) {
            return Result.failure("Refresh token expired");
        }

        return transactionRunner.runReturning(() -> {
            RefreshToken revokedToken = new RefreshToken(
                    stored.id(), stored.userId(), stored.tokenHash(),
                    stored.expiresAt(), true, stored.createdAt()
            );
            refreshTokenRepository.save(revokedToken);

            Optional<User> userOpt = userRepository.findById(stored.userId());
            if (userOpt.isEmpty()) {
                return Result.<AuthTokens>failure("User not found");
            }

            User user = userOpt.get();
            String newAccessToken = jwtProvider.generateAccessToken(user);
            String newRawRefreshToken = jwtProvider.generateRefreshToken();

            RefreshToken newRefreshToken = RefreshToken.create(
                    user.getId(),
                    TokenHash.of(newRawRefreshToken),
                    Instant.now().plus(Duration.ofDays(7))
            );
            refreshTokenRepository.save(newRefreshToken);

            return Result.success(new AuthTokens(
                    newAccessToken,
                    newRawRefreshToken,
                    jwtProvider.getAccessTokenExpirationMs()
            ));
        });
    }
}
