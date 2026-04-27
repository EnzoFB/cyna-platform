package com.cyna.modules.user.interfaces.dto.request;

public record AddressRequest(
        String label,
        String address,
        String address2,
        String zipCode,
        String city,
        String region,
        String countryCode,
        String phone
) {}
