package com.cyna.modules.payment.application.command.processwebhook;

import com.cyna.modules.payment.application.command.processresult.ProcessPaymentResultCommand;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.port.WebhookSignatureException;
import com.cyna.modules.subscription.application.api.SubscriptionCommandApi;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessWebhookCommandHandler implements CommandHandler<ProcessWebhookCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(ProcessWebhookCommandHandler.class);

    private static final String BILLING_REASON_CREATE = "subscription_create";
    private static final String BILLING_REASON_CYCLE = "subscription_cycle";

    private final PaymentGatewayPort paymentGateway;
    private final Mediator mediator;
    private final SubscriptionCommandApi subscriptionCommandApi;

    public ProcessWebhookCommandHandler(PaymentGatewayPort paymentGateway,
                                        Mediator mediator,
                                        SubscriptionCommandApi subscriptionCommandApi) {
        this.paymentGateway = paymentGateway;
        this.mediator = mediator;
        this.subscriptionCommandApi = subscriptionCommandApi;
    }

    @Override
    public Result<Void> handle(ProcessWebhookCommand command) {
        PaymentGatewayPort.StripeWebhookEvent event;
        try {
            event = paymentGateway.parseWebhookEvent(command.payload(), command.sigHeader());
        } catch (WebhookSignatureException e) {
            return Result.failure("INVALID_WEBHOOK_SIGNATURE");
        }

        log.info("Stripe webhook received: type={} reason={} sub={} pi={} stripeStatus={} cape={}",
                event.type(), event.billingReason(), event.subscriptionId(),
                event.paymentIntentId(), event.subscriptionStatus(), event.cancelAtPeriodEnd());

        return switch (event.type()) {
            // First invoice paid → mark Payment SUCCEEDED, OnPaymentSucceeded creates local subscriptions
            case "invoice.paid" -> handleInvoicePaid(event);

            // Failed renewal → mark local subscriptions PAST_DUE; first invoice failure → mark Payment FAILED
            case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);

            // Authoritative subscription state changes from Stripe. Covers:
            //  - cancel_at_period_end toggled (our own writes, customer portal, dashboard)
            //  - status transitions (active ↔ past_due, trial ending…)
            //  - period end shifts (proration, plan change)
            // This is the single anti-drift mechanism — every relevant field is mirrored.
            case "customer.subscription.created",
                 "customer.subscription.updated" -> subscriptionCommandApi.syncFromStripeState(
                    event.subscriptionId(),
                    event.subscriptionStatus(),
                    event.cancelAtPeriodEnd(),
                    event.currentPeriodEnd(),
                    event.canceledAt()
            );

            // Stripe definitively cancelled the subscription → cancel locally (terminal).
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);

            // Legacy PaymentIntent events (kept for backward compatibility)
            case "payment_intent.succeeded" -> event.paymentIntentId() == null
                    ? Result.success()
                    : mediator.send(new ProcessPaymentResultCommand(event.paymentIntentId(), true));
            case "payment_intent.payment_failed" -> event.paymentIntentId() == null
                    ? Result.success()
                    : mediator.send(new ProcessPaymentResultCommand(event.paymentIntentId(), false));

            default -> Result.success();
        };
    }

    private Result<Void> handleInvoicePaid(PaymentGatewayPort.StripeWebhookEvent event) {
        // First invoice for a brand new subscription → existing PI flow
        if (BILLING_REASON_CREATE.equals(event.billingReason())) {
            if (event.paymentIntentId() == null) return Result.success();
            return mediator.send(new ProcessPaymentResultCommand(event.paymentIntentId(), true));
        }

        // Renewal cycle → extend local subscriptions through new period_end
        if (BILLING_REASON_CYCLE.equals(event.billingReason())) {
            if (event.subscriptionId() == null || event.periodEnd() == null) {
                log.warn("invoice.paid (cycle) missing subscription/period_end — ignored");
                return Result.success();
            }
            return subscriptionCommandApi.renewByStripeId(event.subscriptionId(), event.periodEnd());
        }

        // Other billing reasons (e.g. subscription_update, manual) — log and ignore for now
        return Result.success();
    }

    private Result<Void> handleInvoicePaymentFailed(PaymentGatewayPort.StripeWebhookEvent event) {
        // First-invoice failure → mark our Payment as FAILED so the user can retry
        if (BILLING_REASON_CREATE.equals(event.billingReason())) {
            if (event.paymentIntentId() == null) return Result.success();
            return mediator.send(new ProcessPaymentResultCommand(event.paymentIntentId(), false));
        }

        // Renewal payment failed → mark local subscriptions PAST_DUE (Stripe retries via dunning)
        if (BILLING_REASON_CYCLE.equals(event.billingReason())) {
            if (event.subscriptionId() == null) return Result.success();
            return subscriptionCommandApi.markPastDueByStripeId(event.subscriptionId());
        }

        return Result.success();
    }

    private Result<Void> handleSubscriptionDeleted(PaymentGatewayPort.StripeWebhookEvent event) {
        if (event.subscriptionId() == null) return Result.success();
        return subscriptionCommandApi.cancelByStripeId(event.subscriptionId());
    }
}
