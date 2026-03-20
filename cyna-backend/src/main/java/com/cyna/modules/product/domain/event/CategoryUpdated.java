package com.cyna.modules.product.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record CategoryUpdated(
        UUID categoryId,
        String name,
        String description,
        Instant occurredAt
) implements DomainEvent {}
