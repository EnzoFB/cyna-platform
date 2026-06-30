package com.cyna.shared.application;

/**
 * Outbound port for one-way hashing of short, low-entropy OTP codes.
 *
 * <p>The login OTP is 6 numeric digits — only 10^6 possibilities. A plain
 * SHA-256 stored in DB is trivially reversible from a dump (a few seconds
 * with a rainbow table). The implementation wraps the hash in HMAC with a
 * server-side pepper so the leaked DB alone is not enough to recover the
 * raw codes; an attacker would also need to compromise the application
 * config / env vars.</p>
 *
 * <p>This port is OTP-specific by design: refresh tokens, password-reset
 * tokens and email-change tokens are high-entropy random strings that
 * don't benefit from a pepper. They continue to use {@code TokenHash}.</p>
 */
public interface OtpHasher {
    String hash(String otpCode);
}
