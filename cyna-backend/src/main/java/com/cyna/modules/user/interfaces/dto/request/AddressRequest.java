package com.cyna.modules.user.interfaces.dto.request;

public record AddressRequest(
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
        String vatNumber
) {}
