package com.cyna.shared.domain;

import java.time.Instant;

/**
 * Marker interface for domain events.
 * All domain events must implement this interface and be immutable records.
 */
public interface DomainEvent {
    Instant occurredAt();
}
