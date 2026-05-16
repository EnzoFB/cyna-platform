package com.cyna.modules.user.application.command.login.verifyotp;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.model.VerifyOtpOutcome;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.LoginOtpChallenge;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.Role;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.TrustedDevice;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.LoginOtpChallengeRepository;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.repository.TrustedDeviceRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.OtpHasher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class VerifyLoginOtpCommandHandler implements CommandHandler<VerifyLoginOtpCommand, VerifyOtpOutcome> {

    private static final String INVALID_OTP_CHALLENGE = "Invalid OTP challenge";
    private static final String INVALID_OTP_CODE = "Invalid OTP code";
    private static final String TOO_MANY_ATTEMPTS = "Too many attempts";

    private final LoginOtpChallengeRepository loginOtpChallengeRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final JwtProvider jwtProvider;
    private final TransactionRunner transactionRunner;
    private final OtpHasher otpHasher;
    private final long trustedDeviceExpirationDays;

    public VerifyLoginOtpCommandHandler(
            LoginOtpChallengeRepository loginOtpChallengeRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            TrustedDeviceRepository trustedDeviceRepository,
            JwtProvider jwtProvider,
            TransactionRunner transactionRunner,
            OtpHasher otpHasher,
            @Value("${app.auth.trusted-device.expiration-days:30}") long trustedDeviceExpirationDays) {
        this.loginOtpChallengeRepository = loginOtpChallengeRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.jwtProvider = jwtProvider;
        this.transactionRunner = transactionRunner;
        this.otpHasher = otpHasher;
        this.trustedDeviceExpirationDays = trustedDeviceExpirationDays;
    }

    @Override
    public Result<VerifyOtpOutcome> handle(VerifyLoginOtpCommand command) {
        var challengeOpt = loginOtpChallengeRepository.findById(command.challengeId());
        if (challengeOpt.isEmpty()) {
            return Result.failure(INVALID_OTP_CHALLENGE);
        }

        var challenge = challengeOpt.get();
        if (challenge.consumed()) {
            return Result.failure("OTP challenge already used");
        }

        if (challenge.isExpired()) {
            return Result.failure("OTP code expired");
        }

        if (challenge.isLocked()) {
            return Result.failure(TOO_MANY_ATTEMPTS);
        }

        if (!challenge.otpHash().equals(otpHasher.hash(command.otpCode()))) {
            // Persist the failed attempt so the next request sees the bumped
            // counter. Once attempts reaches MAX_ATTEMPTS the challenge is
            // permanently locked even if the user later types the right code.
            return transactionRunner.runReturning(() -> {
                LoginOtpChallenge afterAttempt = challenge.recordFailedAttempt();
                loginOtpChallengeRepository.save(afterAttempt);
                return Result.<VerifyOtpOutcome>failure(
                        afterAttempt.isLocked() ? TOO_MANY_ATTEMPTS : INVALID_OTP_CODE);
            });
        }

        var userOpt = userRepository.findById(challenge.userId());
        if (userOpt.isEmpty()) {
            return Result.failure("User not found");
        }

        User user = userOpt.get();

        return transactionRunner.runReturning(() -> {
            Result<LoginOtpChallenge> consumedResult = challenge.consume();
            if (consumedResult.isFailure()) {
                return Result.failure(consumedResult.getError());
            }

            loginOtpChallengeRepository.save(consumedResult.getValue());

            String accessToken = jwtProvider.generateAccessToken(user);
            String rawRefreshToken = jwtProvider.generateRefreshToken();

            RefreshToken refreshToken = RefreshToken.create(
                    user.getId(),
                    TokenHash.of(rawRefreshToken),
                    Instant.now().plus(Duration.ofHours(jwtProvider.getRefreshTokenExpirationHours()))
            );
            refreshTokenRepository.save(refreshToken);

            // The user just proved possession of their email (OTP) — mark this
            // browser as trusted so future logins from it skip the OTP step.
            // Same scheme as the refresh token: raw value goes to the cookie,
            // hash to the DB.
            //
            // ADMIN accounts are excluded: the back-office requires 2FA on
            // every login, so no trusted device is ever issued for them
            // (null token -> the cookie service emits a no-op/clear cookie).
            String rawDeviceToken = null;
            if (user.getRole() != Role.ADMIN) {
                rawDeviceToken = jwtProvider.generateRefreshToken();
                TrustedDevice device = TrustedDevice.issue(
                        user.getId(),
                        TokenHash.of(rawDeviceToken),
                        Instant.now().plus(Duration.ofDays(trustedDeviceExpirationDays)),
                        command.userAgent()
                );
                trustedDeviceRepository.save(device);
            }

            return Result.success(new VerifyOtpOutcome(
                    new AuthTokens(
                            accessToken,
                            rawRefreshToken,
                            jwtProvider.getAccessTokenExpirationHours()),
                    rawDeviceToken
            ));
        });
    }
}
