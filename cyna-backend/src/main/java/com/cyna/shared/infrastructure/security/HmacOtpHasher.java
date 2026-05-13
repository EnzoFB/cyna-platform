package com.cyna.shared.infrastructure.security;

import com.cyna.shared.application.OtpHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 OTP hasher with a server-side pepper. The pepper is read from
 * the {@code otp.hash-pepper} property (env var {@code OTP_HASH_PEPPER}).
 *
 * <p>Why HMAC and not just {@code SHA-256(pepper || code)}: HMAC is the
 * standard, vetted construction for keyed hashes — it avoids length-extension
 * attacks and other subtle pitfalls of naive concatenation.</p>
 *
 * <p>Fail-fast on missing/short pepper: an empty or trivially short pepper
 * defeats the whole point of this class, so the bean refuses to start. Match
 * the JWT_SECRET convention: a dev default ships in {@code application.yml},
 * prod MUST override via env var.</p>
 */
@Component
public class HmacOtpHasher implements OtpHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_PEPPER_BYTES = 16;

    private final Mac macTemplate;

    public HmacOtpHasher(@Value("${otp.hash-pepper}") String pepper) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException(
                    "otp.hash-pepper is required — set OTP_HASH_PEPPER env var");
        }
        byte[] keyBytes = pepper.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_PEPPER_BYTES) {
            throw new IllegalStateException(
                    "otp.hash-pepper is too short — need at least "
                            + MIN_PEPPER_BYTES + " bytes (got " + keyBytes.length + ")");
        }

        try {
            // Build a template Mac once so each hash() call only pays for an
            // initialised clone, not full key-derivation. Mac is not
            // thread-safe so we clone per invocation.
            Mac probe = Mac.getInstance(HMAC_ALGORITHM);
            probe.init(new SecretKeySpec(keyBytes, HMAC_ALGORITHM));
            this.macTemplate = probe;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable in this JVM", e);
        }
    }

    @Override
    public String hash(String otpCode) {
        if (otpCode == null) {
            throw new IllegalArgumentException("otpCode must not be null");
        }
        try {
            Mac mac = (Mac) macTemplate.clone();
            byte[] digest = mac.doFinal(otpCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("HmacSHA256 Mac is not cloneable on this JVM", e);
        }
    }
}
