package com.cyna.modules.user.application.command.login;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.model.LoginOutcome;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.application.port.OtpCodeGenerator;
import com.cyna.modules.user.application.port.OtpDeliveryPort;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.LoginOtpChallenge;
import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.Role;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.TrustedDevice;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.model.UserStatus;
import com.cyna.modules.user.domain.repository.LoginOtpChallengeRepository;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.repository.TrustedDeviceRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.OtpHasher;
import com.cyna.shared.application.RateLimiter;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Component
public class LoginCommandHandler implements CommandHandler<LoginCommand, LoginOutcome> {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String TOO_MANY_OTP_REQUESTS = "Too many OTP requests";

    private static final String EMAIL_THROTTLE_KEY_PREFIX = "login-otp-email:";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final OtpCodeGenerator otpCodeGenerator;
    private final OtpDeliveryPort otpDeliveryPort;
    private final LoginOtpChallengeRepository loginOtpChallengeRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final TransactionRunner transactionRunner;
    private final RateLimiter rateLimiter;
    private final OtpHasher otpHasher;
    private final int otpCodeLength;
    private final long otpExpirationMinutes;
    private final int emailThrottleMaxPerWindow;
    private final long emailThrottleWindowSeconds;
    private final long trustedDeviceExpirationDays;

    public LoginCommandHandler(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            OtpCodeGenerator otpCodeGenerator,
            OtpDeliveryPort otpDeliveryPort,
            LoginOtpChallengeRepository loginOtpChallengeRepository,
            TrustedDeviceRepository trustedDeviceRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtProvider jwtProvider,
            TransactionRunner transactionRunner,
            RateLimiter rateLimiter,
            OtpHasher otpHasher,
            @Value("${otp.login.code-length:6}") int otpCodeLength,
            @Value("${otp.login.expiration-minutes:5}") long otpExpirationMinutes,
            @Value("${otp.login.email-throttle.max-per-window:3}") int emailThrottleMaxPerWindow,
            @Value("${otp.login.email-throttle.window-seconds:900}") long emailThrottleWindowSeconds,
            @Value("${app.auth.trusted-device.expiration-days:30}") long trustedDeviceExpirationDays) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.otpCodeGenerator = otpCodeGenerator;
        this.otpDeliveryPort = otpDeliveryPort;
        this.loginOtpChallengeRepository = loginOtpChallengeRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProvider = jwtProvider;
        this.transactionRunner = transactionRunner;
        this.rateLimiter = rateLimiter;
        this.otpHasher = otpHasher;
        this.otpCodeLength = otpCodeLength;
        this.otpExpirationMinutes = otpExpirationMinutes;
        this.emailThrottleMaxPerWindow = emailThrottleMaxPerWindow;
        this.emailThrottleWindowSeconds = emailThrottleWindowSeconds;
        this.trustedDeviceExpirationDays = trustedDeviceExpirationDays;
    }

    @Override
    public Result<LoginOutcome> handle(LoginCommand command) {
        Optional<User> userOpt = userRepository.findByEmail(Email.of(command.email()));
        if (userOpt.isEmpty()) {
            return Result.failure(INVALID_CREDENTIALS);
        }

        User user = userOpt.get();
        // Only ACTIVE accounts may authenticate. Blocks deactivated (INACTIVE)
        // and RGPD-anonymized (ANONYMIZED) accounts. Same generic error as a
        // bad password so the response never reveals the account's state.
        if (user.getStatus() != UserStatus.ACTIVE) {
            return Result.failure(INVALID_CREDENTIALS);
        }
        if (!passwordHasher.matches(command.password(), user.getHashedPassword())) {
            return Result.failure(INVALID_CREDENTIALS);
        }

        if (command.requiredRole() != null) {
            Role required = Role.valueOf(command.requiredRole());
            if (user.getRole() != required) {
                return Result.failure(ACCESS_DENIED);
            }
        }

        // "Trust this browser" fast-path. If the request carries a still-valid
        // device cookie tied to this user, skip the OTP step and emit tokens
        // directly. Matches what every modern SaaS does — MFA on a new
        // browser only.
        //
        // Exception: ADMIN accounts (back-office) NEVER skip OTP. The spec
        // mandates 2FA for administrators; a stolen device cookie must not be
        // enough to reach the admin panel, so every admin login goes through
        // the email OTP second factor.
        if (user.getRole() != Role.ADMIN
                && command.trustedDeviceToken() != null && !command.trustedDeviceToken().isBlank()) {
            Optional<TrustedDevice> trustedOpt = trustedDeviceRepository
                    .findByTokenHash(TokenHash.of(command.trustedDeviceToken()));
            if (trustedOpt.isPresent()
                    && trustedOpt.get().userId().equals(user.getId())
                    && !trustedOpt.get().isExpired()) {
                return transactionRunner.runReturning(() -> issueTrustedSession(user, trustedOpt.get()));
            }
            // Cookie present but invalid (different user, expired, or unknown).
            // Silently fall through to the OTP path — the controller will
            // clear the bad cookie on its way out.
        }

        // Throttle by destination email so no mailbox can be flooded with OTPs
        // from an attacker who happens to know the password (credential
        // stuffing). Keyed on the user-stored email (lowercased) rather than
        // the request body so case games don't bypass the bucket. The check
        // sits AFTER credential validation: wrong-password attempts already
        // produce no email, so they don't deserve a slot in this bucket.
        String throttleKey = EMAIL_THROTTLE_KEY_PREFIX
                + user.getEmail().value().toLowerCase(Locale.ROOT);
        RateLimiter.RateLimitDecision decision = rateLimiter.consume(
                throttleKey, emailThrottleMaxPerWindow, emailThrottleWindowSeconds, Instant.now());
        if (!decision.allowed()) {
            return Result.failure(TOO_MANY_OTP_REQUESTS);
        }

        return transactionRunner.runReturning(() -> {
            // Invalidate any prior pending challenge so the user never has more
            // than one valid OTP at a time. Defends against the "spam /login to
            // open N concurrent challenges, then brute-force them in parallel"
            // attack and keeps the mailbox/audit trail tidy.
            loginOtpChallengeRepository.deleteUnconsumedByUserId(user.getId());

            String otpCode = otpCodeGenerator.generateNumericCode(otpCodeLength);
            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(otpExpirationMinutes));
            LoginOtpChallenge challenge = LoginOtpChallenge.create(
                    user.getId(),
                    otpHasher.hash(otpCode),
                    expiresAt
            );

            loginOtpChallengeRepository.save(challenge);
            String lang = command.lang() != null ? command.lang() : "fr";
            otpDeliveryPort.sendLoginOtp(user.getEmail().value(), otpCode, expiresAt, lang);

            return Result.success(new LoginOutcome.Challenge(
                    challenge.id(),
                    Duration.ofMinutes(otpExpirationMinutes).toSeconds()
            ));
        });
    }

    private Result<LoginOutcome> issueTrustedSession(User user, TrustedDevice device) {
        // Slide the device expiry forward so an active user never gets
        // re-challenged. A dormant user (>30 d without login) is the only
        // one whose trust eventually lapses.
        TrustedDevice renewed = device.renew(
                Instant.now().plus(Duration.ofDays(trustedDeviceExpirationDays)));
        trustedDeviceRepository.save(renewed);

        String accessToken = jwtProvider.generateAccessToken(user);
        String rawRefreshToken = jwtProvider.generateRefreshToken();
        RefreshToken refreshToken = RefreshToken.create(
                user.getId(),
                TokenHash.of(rawRefreshToken),
                Instant.now().plus(Duration.ofHours(jwtProvider.getRefreshTokenExpirationHours()))
        );
        refreshTokenRepository.save(refreshToken);

        return Result.success(new LoginOutcome.Authenticated(new AuthTokens(
                accessToken,
                rawRefreshToken,
                jwtProvider.getAccessTokenExpirationHours()
        )));
    }
}
