package com.cyna.shared.application;

import java.time.Instant;

/**
 * Marker for <b>integration events</b>: the published, cross-module
 * notification contract. Unlike a {@link com.cyna.shared.domain.DomainEvent}
 * — which is a module's internal record and must never leave it — an
 * integration event is part of a module's <em>public</em> surface and lives in
 * that module's {@code application.api.event} package.
 *
 * <p>This is the asynchronous half of the modular-monolith seam (the
 * synchronous half being {@code application.api}). Consumers depend only on
 * these contracts, never on a producer's {@code domain.event}. Today they are
 * dispatched in-process through {@link IntegrationEventPublisher}; when a module
 * is extracted into its own service the contract becomes the message payload on
 * the broker and only the publisher/subscriber transport changes — the producer
 * domain and the consumer handlers stay untouched.
 */
public interface IntegrationEvent {

    /** When the originating fact occurred (carried across the boundary for ordering/auditing). */
    Instant occurredAt();
}
