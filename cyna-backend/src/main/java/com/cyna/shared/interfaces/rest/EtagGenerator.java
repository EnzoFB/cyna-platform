package com.cyna.shared.interfaces.rest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.StringJoiner;

/**
 * Generates deterministic ETag values from a set of objects.
 */
public final class EtagGenerator {

    private EtagGenerator() {
    }

    public static String from(Object... parts) {
        StringJoiner joiner = new StringJoiner("|");
        if (parts != null) {
            for (Object part : parts) {
                joiner.add(String.valueOf(part));
            }
        }

        String hash = sha256(joiner.toString());
        return "\"" + hash + "\"";
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
