package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserDeactivated(
        UUID userId,
        String reason,
        Instant occurredAt
) implements DomainEvent {}
