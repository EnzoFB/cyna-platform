package com.cyna.modules.payment.infrastructure.event;

import com.cyna.modules.payment.application.api.PaymentCommandApi;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort.StripeSubscriptionState;
import com.cyna.modules.subscription.application.api.SubscriptionCommandApi;
import com.cyna.modules.subscription.application.api.event.SubscriptionRenewalPreferenceChangedIntegrationEvent;
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
 * <p><b>Stripe stays the single source of truth.</b> We never write the local DB
 * optimistically: we push the change to Stripe, then mirror Stripe's
 * <em>authoritative response</em> back into our read model via
 * {@link SubscriptionCommandApi#syncFromStripeState} — the SAME idempotent path
 * the {@code customer.subscription.updated} webhook uses. So the change is durable
 * within the request (a refresh reads the reconciled DB, not a stale row) while
 * every local state remains a copy of Stripe's. The webhook is retained as the
 * anti-drift backstop for out-of-band changes (Stripe dashboard, customer portal,
 * lifecycle); re-running the same sync is a no-op.
 *
 * <p>The dependency direction (payment → subscription) is the one already used by
 * the webhook handler, so no module cycle is introduced. If Stripe fails, nothing
 * is mirrored (the local DB keeps its previous Stripe-confirmed state) and the
 * webhook remains the fallback.
 */
@Component
public class OnSubscriptionRenewalPreferenceChangedHandler {

    private static final Logger log =
            LoggerFactory.getLogger(OnSubscriptionRenewalPreferenceChangedHandler.class);

    private final PaymentCommandApi paymentCommandApi;
    private final SubscriptionCommandApi subscriptionCommandApi;

    public OnSubscriptionRenewalPreferenceChangedHandler(PaymentCommandApi paymentCommandApi,
                                                         SubscriptionCommandApi subscriptionCommandApi) {
        this.paymentCommandApi = paymentCommandApi;
        this.subscriptionCommandApi = subscriptionCommandApi;
    }

    @EventListener
    public void on(SubscriptionRenewalPreferenceChangedIntegrationEvent event) {
        Result<StripeSubscriptionState> result = paymentCommandApi.setStripeSubscriptionCancelAtPeriodEnd(
                event.stripeSubscriptionId(),
                !event.autoRenew()
        );
        if (result.isFailure()) {
            log.error("[subscription-renewal-pref] Stripe sync failed subId={} stripeSubId={} error={}",
                    event.subscriptionId(), event.stripeSubscriptionId(), result.getError());
            return;
        }

        StripeSubscriptionState state = result.getValue();
        if (state == null) {
            // Nothing authoritative to mirror (blank id, or subscription already
            // terminated / missing at Stripe). The webhook remains the fallback.
            return;
        }

        // Write-through: mirror Stripe's confirmed state into our DB now, via the same
        // idempotent reconciliation the webhook uses. Keeps Stripe the source of truth
        // while making the change durable on refresh.
        Result<Void> mirror = subscriptionCommandApi.syncFromStripeState(
                event.stripeSubscriptionId(),
                state.status(),
                state.cancelAtPeriodEnd(),
                state.currentPeriodEnd(),
                state.canceledAt()
        );
        if (mirror.isFailure()) {
            log.warn("[subscription-renewal-pref] local mirror sync failed subId={} stripeSubId={} error={}",
                    event.subscriptionId(), event.stripeSubscriptionId(), mirror.getError());
        }
    }
}
