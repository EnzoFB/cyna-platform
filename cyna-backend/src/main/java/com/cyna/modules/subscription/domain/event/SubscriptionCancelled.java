package com.cyna.modules.subscription.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionCancelled(
        UUID subscriptionId,
        UUID userId,
        UUID orderId,
        UUID productId,
        Instant occurredAt
) implements DomainEvent {
}
