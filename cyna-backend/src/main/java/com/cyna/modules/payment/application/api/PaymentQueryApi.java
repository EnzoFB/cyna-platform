package com.cyna.modules.payment.application.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only cross-module access to a user's payment-area personal data.
 * Currently exposes the recorded consents for the RGPD Art. 15/20 export
 * (transparency: the user can see exactly what they consented to, when, and
 * from where).
 */
public interface PaymentQueryApi {

    List<PaymentConsentExportView> exportConsentsForUser(UUID userId);

    record PaymentConsentExportView(
            String action,
            String labelVersion,
            String stripePaymentMethodId,
            String ipAddress,
            String userAgent,
            Instant givenAt
    ) {}
}
