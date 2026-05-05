package com.cyna.modules.user.application.api;

import java.util.UUID;

public record UserPaymentView(
        UUID id,
        String email,
        String firstName,
        String lastName
) {}
