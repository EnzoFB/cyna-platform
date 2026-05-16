package com.cyna.modules.payment.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Proof-of-consent entry mandated by Art. 7.1 of GDPR : when the user
 * explicitly accepts the long-term persistence of a payment method (the
 * "Reuse this card" checkbox at checkout), we must be able to prove later
 * <em>what</em> the user agreed to, <em>when</em>, and <em>from where</em>.
 *
 * <p>Immutable by construction — a consent event is a historical fact, never
 * edited.
 */
public final class PaymentConsentLog {

    private final UUID id;
    private final UUID userId;
    private final ConsentAction action;
    private final String labelVersion;
    private final String stripePaymentMethodId;
    private final String ipAddress;
    private final String userAgent;
    private final Instant givenAt;

    private PaymentConsentLog(UUID id, UUID userId, ConsentAction action,
                              String labelVersion, String stripePaymentMethodId,
                              String ipAddress, String userAgent, Instant givenAt) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.labelVersion = labelVersion;
        this.stripePaymentMethodId = stripePaymentMethodId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.givenAt = givenAt;
    }

    public static PaymentConsentLog record(UUID userId, ConsentAction action,
                                           String labelVersion, String stripePaymentMethodId,
                                           String ipAddress, String userAgent) {
        return new PaymentConsentLog(UUID.randomUUID(), userId, action,
                labelVersion, stripePaymentMethodId, ipAddress, userAgent, Instant.now());
    }

    public static PaymentConsentLog reconstitute(UUID id, UUID userId, ConsentAction action,
                                                 String labelVersion, String stripePaymentMethodId,
                                                 String ipAddress, String userAgent, Instant givenAt) {
        return new PaymentConsentLog(id, userId, action, labelVersion,
                stripePaymentMethodId, ipAddress, userAgent, givenAt);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public ConsentAction getAction() { return action; }
    public String getLabelVersion() { return labelVersion; }
    public String getStripePaymentMethodId() { return stripePaymentMethodId; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getGivenAt() { return givenAt; }
}
