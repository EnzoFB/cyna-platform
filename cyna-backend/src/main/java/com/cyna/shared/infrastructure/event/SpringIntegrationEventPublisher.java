package com.cyna.shared.infrastructure.event;

import com.cyna.shared.application.IntegrationEvent;
import com.cyna.shared.application.IntegrationEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * In-process {@link IntegrationEventPublisher}: routes integration events
 * through Spring's {@link ApplicationEventPublisher}, the same bus the domain
 * events use. Consumers subscribe with {@code @TransactionalEventListener}
 * (or {@code @EventListener}) on the concrete integration-event type.
 *
 * <p>Publishing happens synchronously, inside the producer's transaction, so an
 * {@code AFTER_COMMIT} subscriber still fires only once the producing
 * transaction has committed — preserving the existing delivery semantics.
 *
 * <p>When a module is extracted, this single class is replaced by an
 * outbox/broker publisher; nothing else moves.
 */
@Component
public class SpringIntegrationEventPublisher implements IntegrationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringIntegrationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(IntegrationEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
