package com.cyna.modules.payment.application.command.consentlog;

import com.cyna.shared.application.Command;

import java.util.UUID;

/**
 * Records an explicit consent given by the user in the payment area
 * (typically the "Reuse this card" checkbox at checkout). The handler
 * builds the immutable {@code PaymentConsentLog} and writes it to its
 * dedicated table — never updated, never deleted.
 *
 * <p>{@code ipAddress} and {@code userAgent} are captured server-side from
 * the HTTP request so the user can't forge them.
 */
public record LogPaymentConsentCommand(
        UUID userId,
        String labelVersion,
        String stripePaymentMethodId,
        String ipAddress,
        String userAgent
) implements Command<Void> {}
