package com.cyna.modules.subscription.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a renewal charge needs SCA. {@code hostedInvoiceUrl} resumes the 3DS flow on Stripe. */
public record SubscriptionPaymentActionRequiredIntegrationEvent(
        UUID subscriptionId,
        UUID userId,
        UUID orderId,
        UUID productId,
        String hostedInvoiceUrl,
        Instant occurredAt
) implements IntegrationEvent {}
