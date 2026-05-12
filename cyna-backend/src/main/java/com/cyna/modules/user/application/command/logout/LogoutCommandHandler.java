package com.cyna.modules.user.application.command.logout;

import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogoutCommandHandler implements CommandHandler<LogoutCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(LogoutCommandHandler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TransactionRunner transactionRunner;

    public LogoutCommandHandler(RefreshTokenRepository refreshTokenRepository,
                                TransactionRunner transactionRunner) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(LogoutCommand command) {
        return transactionRunner.runReturning(() -> {
            String tokenHash = TokenHash.of(command.refreshToken());

            var storedOpt = refreshTokenRepository.findByTokenHash(tokenHash);
            if (storedOpt.isEmpty()) {
                return Result.<Void>failure("Invalid refresh token");
            }

            RefreshToken stored = storedOpt.get();

            if (command.allDevices()) {
                refreshTokenRepository.revokeAllByUserId(stored.userId());
                log.info("[logout] revoked all sessions userId={}", stored.userId());
            } else {
                RefreshToken revokedToken = new RefreshToken(
                        stored.id(), stored.userId(), stored.tokenHash(),
                        stored.expiresAt(), true, stored.createdAt()
                );
                refreshTokenRepository.save(revokedToken);
                log.info("[logout] revoked current session userId={}", stored.userId());
            }

            return Result.<Void>success(null);
        });
    }
}
