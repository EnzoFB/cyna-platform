package com.cyna.modules.user.application.query.addresses;

import java.time.Instant;
import java.util.UUID;

public record AddressReadModel(
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
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {}
