package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;

public record AddressRequest(
        @NoHtml(message = "First name must not contain HTML")
        String firstName,
        @NoHtml(message = "Last name must not contain HTML")
        String lastName,
        @NoHtml(message = "Label must not contain HTML")
        String label,
        @NoHtml(message = "Address must not contain HTML")
        String address,
        @NoHtml(message = "Address line 2 must not contain HTML")
        String address2,
        @NoHtml(message = "Zip code must not contain HTML")
        String zipCode,
        @NoHtml(message = "City must not contain HTML")
        String city,
        @NoHtml(message = "Region must not contain HTML")
        String region,
        @NoHtml(message = "Country code must not contain HTML")
        String countryCode,
        @NoHtml(message = "Phone must not contain HTML")
        String phone,
        @NoHtml(message = "Company must not contain HTML")
        String company,
        @NoHtml(message = "VAT number must not contain HTML")
        String vatNumber
) {}
