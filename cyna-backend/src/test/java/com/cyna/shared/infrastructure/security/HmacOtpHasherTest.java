package com.cyna.shared.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacOtpHasherTest {

    private static final String VALID_PEPPER = "test-pepper-at-least-16-bytes-long";

    @Test
    void should_produce_a_hex_sha256_sized_digest() {
        var hasher = new HmacOtpHasher(VALID_PEPPER);

        String digest = hasher.hash("123456");

        // HMAC-SHA256 → 32 bytes → 64 hex chars.
        assertThat(digest).hasSize(64).matches("^[0-9a-f]{64}$");
    }

    @Test
    void should_be_deterministic_for_the_same_pepper_and_code() {
        var hasher = new HmacOtpHasher(VALID_PEPPER);

        assertThat(hasher.hash("123456")).isEqualTo(hasher.hash("123456"));
    }

    @Test
    void should_produce_different_digests_for_different_peppers() {
        // The whole point of the pepper: a DB dump without the server-side key
        // can't be brute-forced back to the raw code, because flipping the key
        // changes the digest entirely.
        var a = new HmacOtpHasher(VALID_PEPPER);
        var b = new HmacOtpHasher("different-pepper-at-least-16-bytes");

        assertThat(a.hash("123456")).isNotEqualTo(b.hash("123456"));
    }

    @Test
    void should_reject_null_or_blank_pepper() {
        assertThatThrownBy(() -> new HmacOtpHasher(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OTP_HASH_PEPPER");

        assertThatThrownBy(() -> new HmacOtpHasher("   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OTP_HASH_PEPPER");
    }

    @Test
    void should_reject_short_pepper_below_min_bytes() {
        assertThatThrownBy(() -> new HmacOtpHasher("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short");
    }
}
