package com.cyna.modules.user.application.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The user-owned slice of a personal-data export (RGPD Art. 15/20): the account
 * identity, postal addresses and terms-of-service consent history. Cross-module
 * data (orders, subscriptions, payment consents) is assembled by the caller
 * from the respective modules' APIs.
 */
public record UserPersonalDataView(
        Account account,
        List<Address> addresses,
        List<Consent> termsConsents
) {
    public record Account(
            UUID id,
            String email,
            String firstName,
            String lastName,
            String company,
            String role,
            String status,
            Instant createdAt
    ) {}

    public record Address(
            UUID id,
            String firstName,
            String lastName,
            String label,
            String address,
            String address2,
            String zipCode,
            String city,
            String region,
            String countryCode,
            String phone,
            String company,
            String vatNumber,
            boolean isDefault
    ) {}

    public record Consent(
            String action,
            String labelVersion,
            String ipAddress,
            String userAgent,
            Instant givenAt
    ) {}
}
