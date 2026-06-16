package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;

public record UpdateProfileRequest(
        @NoHtml(message = "First name must not contain HTML")
        String firstName,
        @NoHtml(message = "Last name must not contain HTML")
        String lastName,
        @NoHtml(message = "Company must not contain HTML")
        String company
) {}
