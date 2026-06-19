package com.cyna.modules.subscription.infrastructure.event;

import com.cyna.modules.subscription.application.api.event.SubscriptionAutoRenewReminderDueIntegrationEvent;
import com.cyna.modules.subscription.application.api.event.SubscriptionCancelledIntegrationEvent;
import com.cyna.modules.subscription.application.api.event.SubscriptionPaymentActionRequiredIntegrationEvent;
import com.cyna.modules.subscription.application.api.event.SubscriptionPaymentFailedIntegrationEvent;
import com.cyna.modules.subscription.application.api.event.SubscriptionRenewalPreferenceChangedIntegrationEvent;
import com.cyna.modules.subscription.application.api.event.SubscriptionTrialWillEndIntegrationEvent;
import com.cyna.modules.subscription.domain.event.SubscriptionAutoRenewReminderDue;
import com.cyna.modules.subscription.domain.event.SubscriptionCancelled;
import com.cyna.modules.subscription.domain.event.SubscriptionPaymentActionRequired;
import com.cyna.modules.subscription.domain.event.SubscriptionPaymentFailed;
import com.cyna.modules.subscription.domain.event.SubscriptionRenewalPreferenceChanged;
import com.cyna.modules.subscription.domain.event.SubscriptionTrialWillEnd;
import com.cyna.shared.application.IntegrationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates the {@code subscription} module's internal domain events into the
 * published integration contract. Synchronous, in-transaction — see
 * {@code com.cyna.modules.user.infrastructure.event.UserIntegrationEventTranslator}
 * for the rationale.
 */
@Component
public class SubscriptionIntegrationEventTranslator {

    private final IntegrationEventPublisher publisher;

    public SubscriptionIntegrationEventTranslator(IntegrationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void on(SubscriptionCancelled e) {
        publisher.publish(new SubscriptionCancelledIntegrationEvent(
                e.subscriptionId(), e.userId(), e.orderId(), e.productId(), e.occurredAt()));
    }

    @EventListener
    public void on(SubscriptionPaymentFailed e) {
        publisher.publish(new SubscriptionPaymentFailedIntegrationEvent(
                e.subscriptionId(), e.userId(), e.orderId(), e.productId(), e.occurredAt()));
    }

    @EventListener
    public void on(SubscriptionPaymentActionRequired e) {
        publisher.publish(new SubscriptionPaymentActionRequiredIntegrationEvent(
                e.subscriptionId(), e.userId(), e.orderId(), e.productId(), e.hostedInvoiceUrl(), e.occurredAt()));
    }

    @EventListener
    public void on(SubscriptionAutoRenewReminderDue e) {
        publisher.publish(new SubscriptionAutoRenewReminderDueIntegrationEvent(
                e.subscriptionId(), e.userId(), e.email(), e.firstName(),
                e.productName(), e.renewalDate(), e.lang(), e.occurredAt()));
    }

    @EventListener
    public void on(SubscriptionTrialWillEnd e) {
        publisher.publish(new SubscriptionTrialWillEndIntegrationEvent(
                e.subscriptionId(), e.userId(), e.productId(), e.trialEndAt(), e.occurredAt()));
    }

    @EventListener
    public void on(SubscriptionRenewalPreferenceChanged e) {
        publisher.publish(new SubscriptionRenewalPreferenceChangedIntegrationEvent(
                e.subscriptionId(), e.stripeSubscriptionId(), e.autoRenew(), e.occurredAt()));
    }
}
