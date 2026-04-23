package com.cyna.modules.user.interfaces.dto.request;

public record UpdateProfileRequest(
        String firstName,
        String lastName,
        String company
) {}
