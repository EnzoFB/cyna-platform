package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;

public record RequestEmailChangeRequest(
        @NoHtml(message = "Email must not contain HTML")
        String newEmail,
        @NoHtml(message = "Language must not contain HTML")
        String lang
) {}
