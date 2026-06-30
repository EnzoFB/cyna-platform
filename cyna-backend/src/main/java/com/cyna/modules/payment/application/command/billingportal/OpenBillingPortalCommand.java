package com.cyna.modules.payment.application.command.billingportal;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record OpenBillingPortalCommand(
        UUID userId,
        String returnUrl
) implements Command<String> {}
