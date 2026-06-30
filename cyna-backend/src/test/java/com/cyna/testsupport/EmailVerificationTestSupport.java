package com.cyna.testsupport;

import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.EmailVerificationToken;
import com.cyna.modules.user.domain.model.TokenHash;
import com.cyna.modules.user.domain.repository.EmailVerificationTokenRepository;
import com.cyna.modules.user.domain.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;

/**
 * Shared test helper for the email-verification flow.
 *
 * <p>Since {@code POST /auth/register} now creates a PENDING_VERIFICATION
 * account and no longer auto-logs the user in, integration tests that need an
 * authenticated session must activate the account first. This helper seeds a
 * known raw verification token directly (the real raw token is only mailed) so
 * a test can then drive {@code POST /auth/confirm-email}, which activates the
 * account and returns the auth payload (access + refresh tokens).</p>
 */
public final class EmailVerificationTestSupport {

    private EmailVerificationTestSupport() {}

    /**
     * Seeds a single known raw verification token for an already-registered
     * (PENDING) user and returns the raw value, ready to POST to
     * {@code /auth/confirm-email}.
     */
    public static String seedVerificationToken(UserRepository userRepository,
                                               EmailVerificationTokenRepository tokenRepository,
                                               String email) {
        var user = userRepository.findByEmail(Email.of(email)).orElseThrow();
        tokenRepository.deleteUnconsumedByUserId(user.getId());
        String raw = "verify-known-" + System.nanoTime();
        tokenRepository.save(EmailVerificationToken.create(
                user.getId(),
                TokenHash.of(raw),
                Instant.now().plus(Duration.ofHours(24))
        ));
        return raw;
    }
}
