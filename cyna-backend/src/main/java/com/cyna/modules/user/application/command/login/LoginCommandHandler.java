package com.cyna.modules.user.application.command.login;

import com.cyna.modules.user.application.model.LoginChallenge;
import com.cyna.modules.user.application.port.OtpCodeGenerator;
import com.cyna.modules.user.application.port.OtpDeliveryPort;
import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.LoginOtpChallenge;
import com.cyna.modules.user.domain.model.Role;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.LoginOtpChallengeRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class LoginCommandHandler implements CommandHandler<LoginCommand, LoginChallenge> {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    public static final String ACCESS_DENIED = "Access denied";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final OtpCodeGenerator otpCodeGenerator;
    private final OtpDeliveryPort otpDeliveryPort;
    private final LoginOtpChallengeRepository loginOtpChallengeRepository;
    private final TransactionRunner transactionRunner;
    private final int otpCodeLength;
    private final long otpExpirationMinutes;

    public LoginCommandHandler(UserRepository userRepository,
                               PasswordHasher passwordHasher,
                               OtpCodeGenerator otpCodeGenerator,
                               OtpDeliveryPort otpDeliveryPort,
                               LoginOtpChallengeRepository loginOtpChallengeRepository,
                               TransactionRunner transactionRunner,
                               @Value("${otp.login.code-length:6}") int otpCodeLength,
                               @Value("${otp.login.expiration-minutes:5}") long otpExpirationMinutes) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.otpCodeGenerator = otpCodeGenerator;
        this.otpDeliveryPort = otpDeliveryPort;
        this.loginOtpChallengeRepository = loginOtpChallengeRepository;
        this.transactionRunner = transactionRunner;
        this.otpCodeLength = otpCodeLength;
        this.otpExpirationMinutes = otpExpirationMinutes;
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

        return transactionRunner.runReturning(() -> {
            String otpCode = otpCodeGenerator.generateNumericCode(otpCodeLength);
            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(otpExpirationMinutes));
            LoginOtpChallenge challenge = LoginOtpChallenge.create(
                    user.getId(),
                    TokenHash.of(otpCode),
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
