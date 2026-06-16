package com.cyna.modules.payment.infrastructure.stripe;

import java.util.Set;

/**
 * Shared classification of VAT numbers by their 2-letter country prefix. Used by
 * both the customer {@code tax_id} push and the tax-calculation adapter to pick
 * the right Stripe tax-id type, without duplicating the prefix table.
 */
final class VatNumbers {

    /**
     * EU VAT prefixes. Note the VAT-specific spellings: Greece is {@code EL}
     * (not GR) and Northern Ireland is {@code XI}.
     */
    private static final Set<String> EU_PREFIXES = Set.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE",
            "EL", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT",
            "RO", "SK", "SI", "ES", "SE", "XI");

    enum Region { EU, GB, CH, NO, UNKNOWN }

    private VatNumbers() {}

    /** Strips whitespace and uppercases — Stripe expects e.g. {@code FR12345678901}. */
    static String normalize(String rawVatNumber) {
        return rawVatNumber.replaceAll("\\s", "").toUpperCase();
    }

    /** Region of a normalized VAT number, or {@link Region#UNKNOWN} if the prefix isn't handled. */
    static Region regionOf(String normalizedVatNumber) {
        if (normalizedVatNumber == null || normalizedVatNumber.length() < 3) {
            return Region.UNKNOWN;
        }
        String prefix = normalizedVatNumber.substring(0, 2);
        if (EU_PREFIXES.contains(prefix)) {
            return Region.EU;
        }
        return switch (prefix) {
            case "GB" -> Region.GB;
            case "CH" -> Region.CH;
            case "NO" -> Region.NO;
            default -> Region.UNKNOWN;
        };
    }
}
