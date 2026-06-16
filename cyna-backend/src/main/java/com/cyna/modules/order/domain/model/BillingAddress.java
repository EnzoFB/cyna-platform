package com.cyna.modules.order.domain.model;

public record BillingAddress(
        String line1,
        String city,
        String zipCode,
        String countryCode
) {
}
