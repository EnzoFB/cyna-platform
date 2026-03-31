package com.cyna.modules.product.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record CategoryDeleted(
        UUID categoryId,
        Instant occurredAt
) implements DomainEvent {}
