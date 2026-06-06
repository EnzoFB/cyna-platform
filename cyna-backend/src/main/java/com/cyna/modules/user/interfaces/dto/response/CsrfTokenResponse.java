package com.cyna.modules.user.interfaces.dto.response;

public record CsrfTokenResponse(
        String token,
        String headerName
) {
}
