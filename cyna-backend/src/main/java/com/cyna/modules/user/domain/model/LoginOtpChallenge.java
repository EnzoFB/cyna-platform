package com.cyna.modules.user.domain.model;

import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Result;

import java.time.Instant;
import java.util.UUID;

public record LoginOtpChallenge(
        UUID id,
        UUID userId,
        String otpHash,
        Instant expiresAt,
        boolean consumed,
        Instant createdAt,
        Instant consumedAt
) {
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
                null
        );
    }

    public static LoginOtpChallenge reconstitute(
            UUID id,
            UUID userId,
            String otpHash,
            Instant expiresAt,
            boolean consumed,
            Instant createdAt,
            Instant consumedAt
    ) {
        Guard.againstNull(id, "id");
        Guard.againstNull(userId, "userId");
        Guard.againstNullOrBlank(otpHash, "otpHash");
        Guard.againstNull(expiresAt, "expiresAt");
        Guard.againstNull(createdAt, "createdAt");

        return new LoginOtpChallenge(id, userId, otpHash, expiresAt, consumed, createdAt, consumedAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public Result<LoginOtpChallenge> consume() {
        if (consumed) {
            return Result.failure("OTP challenge already used");
        }
        if (isExpired()) {
            return Result.failure("OTP code expired");
        }

        return Result.success(new LoginOtpChallenge(
                id,
                userId,
                otpHash,
                expiresAt,
                true,
                createdAt,
                Instant.now()
        ));
    }
}
