package com.cyna.modules.payment.infrastructure.event;

import com.cyna.modules.payment.application.api.PaymentCommandApi;
import com.cyna.modules.subscription.domain.event.SubscriptionRenewalPreferenceChanged;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Payment owns the Stripe relationship, so it reacts to a subscription's
 * renewal-preference change by syncing {@code cancel_at_period_end} on the
 * Stripe Subscription ({@code = !autoRenew}). Listening to the subscription
 * event (instead of subscription calling payment) keeps the subscription module
 * free of any payment dependency — the edge that would otherwise close the
 * payment↔subscription cycle.
 *
 * <p>Dispatch is synchronous (no surrounding transaction), so Stripe is updated
 * within the originating request. A Stripe failure is logged here and on the
 * gateway; the {@code customer.subscription.updated} webhook remains the
 * authoritative reconciliation path, so this stays best-effort.
 */
@Component
public class OnSubscriptionRenewalPreferenceChangedHandler {

    private static final Logger log =
            LoggerFactory.getLogger(OnSubscriptionRenewalPreferenceChangedHandler.class);

    private final PaymentCommandApi paymentCommandApi;

    public OnSubscriptionRenewalPreferenceChangedHandler(PaymentCommandApi paymentCommandApi) {
        this.paymentCommandApi = paymentCommandApi;
    }

    @EventListener
    public void on(SubscriptionRenewalPreferenceChanged event) {
        Result<Void> result = paymentCommandApi.setStripeSubscriptionCancelAtPeriodEnd(
                event.stripeSubscriptionId(),
                !event.autoRenew()
        );
        if (result.isFailure()) {
            log.error("[subscription-renewal-pref] Stripe sync failed subId={} stripeSubId={} error={}",
                    event.subscriptionId(), event.stripeSubscriptionId(), result.getError());
        }
    }
}
