package com.cyna.modules.product.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record CategoryCreated(
        UUID categoryId,
        String name,
        Instant occurredAt
) implements DomainEvent {}
