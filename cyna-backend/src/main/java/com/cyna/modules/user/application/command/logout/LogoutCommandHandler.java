package com.cyna.modules.user.application.command.logout;

import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LogoutCommandHandler implements CommandHandler<LogoutCommand, Void> {

    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutCommandHandler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public Result<Void> handle(LogoutCommand command) {
        String tokenHash = TokenHash.of(command.refreshToken());

        Optional<RefreshToken> storedOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        if (storedOpt.isEmpty()) {
            return Result.failure("Invalid refresh token");
        }

        RefreshToken stored = storedOpt.get();

        RefreshToken revokedToken = new RefreshToken(
                stored.id(), stored.userId(), stored.tokenHash(),
                stored.expiresAt(), true, stored.createdAt()
        );
        refreshTokenRepository.save(revokedToken);

        return Result.success(null);
    }
}
