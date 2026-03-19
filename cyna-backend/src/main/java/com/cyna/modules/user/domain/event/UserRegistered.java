package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserRegistered(
        UUID userId,
        String email,
        String role,
        Instant occurredAt
) implements DomainEvent {}
