package com.cyna.modules.order.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelled(
        UUID orderId,
        UUID userId,
        String reason,
        Instant occurredAt
) implements DomainEvent {
}
