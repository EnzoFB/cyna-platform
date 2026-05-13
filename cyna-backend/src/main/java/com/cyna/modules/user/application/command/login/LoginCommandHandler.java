package com.cyna.modules.user.application.command.login;

import com.cyna.modules.user.application.model.LoginChallenge;
import com.cyna.modules.user.application.port.OtpCodeGenerator;
import com.cyna.modules.user.application.port.OtpDeliveryPort;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.LoginOtpChallenge;
import com.cyna.modules.user.domain.model.Role;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.LoginOtpChallengeRepository;
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
public class LoginCommandHandler implements CommandHandler<LoginCommand, LoginChallenge> {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String TOO_MANY_OTP_REQUESTS = "Too many OTP requests";

    private static final String EMAIL_THROTTLE_KEY_PREFIX = "login-otp-email:";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final OtpCodeGenerator otpCodeGenerator;
    private final OtpDeliveryPort otpDeliveryPort;
    private final LoginOtpChallengeRepository loginOtpChallengeRepository;
    private final TransactionRunner transactionRunner;
    private final RateLimiter rateLimiter;
    private final OtpHasher otpHasher;
    private final int otpCodeLength;
    private final long otpExpirationMinutes;
    private final int emailThrottleMaxPerWindow;
    private final long emailThrottleWindowSeconds;

    public LoginCommandHandler(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            OtpCodeGenerator otpCodeGenerator,
            OtpDeliveryPort otpDeliveryPort,
            LoginOtpChallengeRepository loginOtpChallengeRepository,
            TransactionRunner transactionRunner,
            RateLimiter rateLimiter,
            OtpHasher otpHasher,
            @Value("${otp.login.code-length:6}") int otpCodeLength,
            @Value("${otp.login.expiration-minutes:5}") long otpExpirationMinutes,
            @Value("${otp.login.email-throttle.max-per-window:3}") int emailThrottleMaxPerWindow,
            @Value("${otp.login.email-throttle.window-seconds:900}") long emailThrottleWindowSeconds) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.otpCodeGenerator = otpCodeGenerator;
        this.otpDeliveryPort = otpDeliveryPort;
        this.loginOtpChallengeRepository = loginOtpChallengeRepository;
        this.transactionRunner = transactionRunner;
        this.rateLimiter = rateLimiter;
        this.otpHasher = otpHasher;
        this.otpCodeLength = otpCodeLength;
        this.otpExpirationMinutes = otpExpirationMinutes;
        this.emailThrottleMaxPerWindow = emailThrottleMaxPerWindow;
        this.emailThrottleWindowSeconds = emailThrottleWindowSeconds;
    }

    @Override
    public Result<LoginChallenge> handle(LoginCommand command) {
        Optional<User> userOpt = userRepository.findByEmail(Email.of(command.email()));
        if (userOpt.isEmpty()) {
            return Result.failure(INVALID_CREDENTIALS);
        }

        User user = userOpt.get();
        if (!passwordHasher.matches(command.password(), user.getHashedPassword())) {
            return Result.failure(INVALID_CREDENTIALS);
        }

        if (command.requiredRole() != null) {
            Role required = Role.valueOf(command.requiredRole());
            if (user.getRole() != required) {
                return Result.failure(ACCESS_DENIED);
            }
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

            return Result.success(new LoginChallenge(
                    challenge.id(),
                    Duration.ofMinutes(otpExpirationMinutes).toSeconds()
            ));
        });
    }
}
