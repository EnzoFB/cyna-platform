package com.cyna.modules.user.domain.model;

import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Result;

import java.time.Instant;
import java.util.UUID;

/**
 * One-shot OTP challenge issued after credentials are validated.
 *
 * <p>The raw 6-digit code is stored as a SHA-256 hash. The challenge is
 * single-use ({@code consumed}) and time-limited ({@code expiresAt}). To
 * defeat brute-force, each wrong code increments {@link #attempts}; once
 * it reaches {@link #MAX_ATTEMPTS} the challenge is permanently locked
 * even if it has not expired and even if a correct code is submitted
 * afterwards. The user must restart the login flow.</p>
 */
public record LoginOtpChallenge(
        UUID id,
        UUID userId,
        String otpHash,
        Instant expiresAt,
        boolean consumed,
        Instant createdAt,
        Instant consumedAt,
        int attempts
) {

    public static final int MAX_ATTEMPTS = 5;

    public static LoginOtpChallenge create(UUID userId, String otpHash, Instant expiresAt) {
        Guard.againstNull(userId, "userId");
        Guard.againstNullOrBlank(otpHash, "otpHash");
        Guard.againstNull(expiresAt, "expiresAt");

        var now = Instant.now();
        return new LoginOtpChallenge(
                UUID.randomUUID(),
                userId,
                otpHash,
                expiresAt,
                false,
                now,
                null,
                0
        );
    }

    public static LoginOtpChallenge reconstitute(
            UUID id,
            UUID userId,
            String otpHash,
            Instant expiresAt,
            boolean consumed,
            Instant createdAt,
            Instant consumedAt,
            int attempts
    ) {
        Guard.againstNull(id, "id");
        Guard.againstNull(userId, "userId");
        Guard.againstNullOrBlank(otpHash, "otpHash");
        Guard.againstNull(expiresAt, "expiresAt");
        Guard.againstNull(createdAt, "createdAt");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must be >= 0");
        }

        return new LoginOtpChallenge(id, userId, otpHash, expiresAt, consumed, createdAt, consumedAt, attempts);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isLocked() {
        return attempts >= MAX_ATTEMPTS;
    }

    public LoginOtpChallenge recordFailedAttempt() {
        return new LoginOtpChallenge(
                id,
                userId,
                otpHash,
                expiresAt,
                consumed,
                createdAt,
                consumedAt,
                attempts + 1
        );
    }

    public Result<LoginOtpChallenge> consume() {
        if (consumed) {
            return Result.failure("OTP challenge already used");
        }
        if (isExpired()) {
            return Result.failure("OTP code expired");
        }
        if (isLocked()) {
            return Result.failure("Too many attempts");
        }

        return Result.success(new LoginOtpChallenge(
                id,
                userId,
                otpHash,
                expiresAt,
                true,
                createdAt,
                Instant.now(),
                attempts
        ));
    }
}
