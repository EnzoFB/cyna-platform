package com.cyna.shared.application;

import com.cyna.shared.domain.DomainEvent;

import java.util.List;

/**
 * Publishes domain events to interested handlers.
 * Implementation uses Spring's ApplicationEventPublisher.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);

    void publishAll(List<DomainEvent> events);
}
