package com.cyna.shared.application;

/**
 * Publishes {@link IntegrationEvent}s to other modules. This is the seam that
 * becomes a message-broker producer when a module is extracted: today the
 * implementation routes events through the in-process bus, tomorrow it writes
 * to an outbox / publishes to a topic — without any change to the translators
 * that call it.
 */
public interface IntegrationEventPublisher {

    void publish(IntegrationEvent event);
}
