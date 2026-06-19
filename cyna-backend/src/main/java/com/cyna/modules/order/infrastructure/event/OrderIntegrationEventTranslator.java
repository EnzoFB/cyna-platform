package com.cyna.modules.order.infrastructure.event;

import com.cyna.modules.order.application.api.event.OrderPaidIntegrationEvent;
import com.cyna.modules.order.domain.event.OrderPaid;
import com.cyna.shared.application.IntegrationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates the {@code order} module's internal domain events into the
 * published integration contract. Synchronous, in-transaction — see
 * {@code com.cyna.modules.user.infrastructure.event.UserIntegrationEventTranslator}
 * for the rationale.
 */
@Component
public class OrderIntegrationEventTranslator {

    private final IntegrationEventPublisher publisher;

    public OrderIntegrationEventTranslator(IntegrationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void on(OrderPaid e) {
        publisher.publish(new OrderPaidIntegrationEvent(
                e.orderId(), e.userId(), e.subtotalHt(), e.lang(), e.occurredAt()));
    }
}
