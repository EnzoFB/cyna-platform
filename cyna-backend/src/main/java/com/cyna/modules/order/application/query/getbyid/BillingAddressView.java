package com.cyna.modules.order.application.query.getbyid;

import com.cyna.modules.order.domain.model.BillingAddress;

public record BillingAddressView(
        String line1,
        String city,
        String zipCode,
        String countryCode
) {
    public static BillingAddressView from(BillingAddress ba) {
        if (ba == null) return null;
        return new BillingAddressView(ba.line1(), ba.city(), ba.zipCode(), ba.countryCode());
    }
}
