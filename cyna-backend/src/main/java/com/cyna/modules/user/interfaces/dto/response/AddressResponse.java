package com.cyna.modules.user.interfaces.dto.response;

import com.cyna.modules.user.application.query.addresses.AddressReadModel;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
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
) {
    public static AddressResponse from(AddressReadModel m) {
        return new AddressResponse(
                m.id(), m.firstName(), m.lastName(), m.label(), m.address(), m.address2(),
                m.zipCode(), m.city(), m.region(), m.countryCode(), m.phone(),
                m.company(), m.vatNumber(), m.isDefault(), m.createdAt(), m.updatedAt()
        );
    }
}
