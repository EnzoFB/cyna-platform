package com.cyna.modules.user.application.command.login;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.Email;
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
public class LoginCommandHandler implements CommandHandler<LoginCommand, AuthTokens> {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TransactionRunner transactionRunner;

    public LoginCommandHandler(UserRepository userRepository,
                               PasswordHasher passwordHasher,
                               JwtProvider jwtProvider,
                               RefreshTokenRepository refreshTokenRepository,
                               TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<AuthTokens> handle(LoginCommand command) {
        Optional<User> userOpt = userRepository.findByEmail(Email.of(command.email()));
        if (userOpt.isEmpty()) {
            return Result.failure(INVALID_CREDENTIALS);
        }

        User user = userOpt.get();
        if (!passwordHasher.matches(command.password(), user.getHashedPassword())) {
            return Result.failure(INVALID_CREDENTIALS);
        }

        return transactionRunner.runReturning(() -> {
            String accessToken = jwtProvider.generateAccessToken(user);
            String rawRefreshToken = jwtProvider.generateRefreshToken();

            RefreshToken refreshToken = RefreshToken.create(
                    user.getId(),
                    TokenHash.of(rawRefreshToken),
                    Instant.now().plus(Duration.ofHours(jwtProvider.getRefreshTokenExpirationHours()))
            );
            refreshTokenRepository.save(refreshToken);

            return Result.success(new AuthTokens(
                    accessToken,
                    rawRefreshToken,
                    jwtProvider.getAccessTokenExpirationHours()
            ));
        });
    }
}
