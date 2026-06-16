package com.cyna.modules.payment.infrastructure.event;

import com.cyna.modules.order.application.api.OrderCommandApi;
import com.cyna.modules.order.application.api.OrderPaymentView;
import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.domain.event.PaymentSucceeded;
import com.cyna.modules.subscription.application.api.SubscriptionCommandApi;
import com.cyna.modules.subscription.application.api.SubscriptionPaymentPayload;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Webhook reconciliation path for {@link PaymentSucceeded}.
 *
 * <p>Provisioning has two channels that converge on the same end state, made
 * safe by idempotency at every step:
 * <ul>
 *   <li><b>Optimistic synchronous</b> — {@code FinalizePaymentCommandHandler}
 *       runs during the customer's {@code POST /payments/finalize} call. It
 *       creates the Stripe Subscriptions, then in a single local transaction
 *       marks the Order PAID, the Payment SUCCEEDED and persists the local
 *       Subscriptions. When that transaction commits, this listener fires but
 *       finds the Order already PAID and short-circuits.</li>
 *   <li><b>Authoritative asynchronous</b> — this handler. Stripe is the source
 *       of truth that money actually moved. When {@code invoice.paid}
 *       (billing_reason {@code subscription_create}) or the legacy
 *       {@code payment_intent.succeeded} webhook arrives, it routes through
 *       {@code ProcessPaymentResultCommandHandler} → {@code markSucceeded()} →
 *       this listener.</li>
 * </ul>
 *
 * <p><b>Why this must stay even though finalize provisions eagerly:</b> the
 * Stripe calls in finalize happen <i>before</i> its local persistence
 * transaction. If Stripe charges the customer but that transaction then fails
 * or rolls back, the customer is debited while the Order stays unpaid and no
 * Subscription exists locally. The {@code invoice.paid} webhook is what
 * reconciles that window — this handler marks the Order PAID and creates the
 * local Subscriptions so a charged customer is never left without service. It
 * also covers the race where the webhook is delivered before finalize commits.
 *
 * <p>Idempotency guarantees overlap is harmless: we detect "already
 * provisioned" via the Order status (PAID/FULFILLED) and return early;
 * {@code markOrderAsPaid} and {@code createFromPayment} are themselves
 * idempotent on re-entry.
 */
@Component
public class PaymentSucceededReconciliationHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentSucceededReconciliationHandler.class);

    private final OrderCommandApi orderCommandApi;
    private final OrderQueryApi orderQueryApi;
    private final SubscriptionCommandApi subscriptionCommandApi;
    private final TransactionRunner transactionRunner;

    public PaymentSucceededReconciliationHandler(OrderCommandApi orderCommandApi,
                                                 OrderQueryApi orderQueryApi,
                                                 SubscriptionCommandApi subscriptionCommandApi,
                                                 TransactionRunner transactionRunner) {
        this.orderCommandApi = orderCommandApi;
        this.orderQueryApi = orderQueryApi;
        this.subscriptionCommandApi = subscriptionCommandApi;
        this.transactionRunner = transactionRunner;
    }

    @EventListener
    public void on(PaymentSucceeded event) {
        transactionRunner.run(() -> {
            OrderPaymentView order = orderQueryApi
                    .findOrderForPayment(event.orderId(), event.userId())
                    .orElse(null);

            if (order == null) {
                log.error("[payment-reconcile] order not found — orderId={} userId={}",
                        event.orderId(), event.userId());
                throw new IllegalStateException(
                        "Order " + event.orderId() + " not found");
            }

            // Already provisioned by the synchronous finalize path (Order marked
            // PAID in that transaction). Nothing to reconcile — skip.
            if ("PAID".equals(order.status()) || "FULFILLED".equals(order.status())) {
                return;
            }

            // Reconciliation: finalize did not (or could not) persist locally
            // after Stripe confirmed the charge. Mark PAID and create one
            // Subscription per OrderLine using the Stripe Subscription id carried
            // by the PaymentSucceeded event.
            Result<Void> paidResult = orderCommandApi.markOrderAsPaid(event.orderId());
            if (paidResult.isFailure()) {
                log.error("[payment-reconcile] mark order PAID failed — orderId={} userId={} error={}",
                        event.orderId(), event.userId(), paidResult.getError());
                throw new IllegalStateException(
                        "Cannot mark order " + event.orderId() + " as paid: " + paidResult.getError());
            }

            Instant startAt = event.occurredAt();
            for (OrderPaymentView.OrderLineView line : order.lines()) {
                BillingCycle billingCycle = BillingCycle.valueOf(line.billingCycle());
                Instant endAt = calculateEndDate(startAt, billingCycle);

                var payload = new SubscriptionPaymentPayload(
                        event.userId(),
                        event.orderId(),
                        null, // webhook path: PaymentSucceeded does not carry order_line_id
                        line.productId(),
                        line.productName(),
                        line.productCategory(),
                        billingCycle,
                        line.quantity(),
                        line.unitPrice(),
                        order.currency(),
                        startAt,
                        endAt,
                        endAt,
                        // V14: N Stripe Subscriptions per Order — a single field on
                        // PaymentSucceeded can't carry them all. The local sub is
                        // created with null stripeSubscriptionId here and the
                        // `customer.subscription.updated` webhook reconciles it via
                        // the cyna_order_line_id metadata on the Stripe Subscription.
                        null,
                        null
                );

                Result<?> result = subscriptionCommandApi.createFromPayment(payload);
                if (result.isFailure()) {
                    log.error("[payment-reconcile] create subscription failed — orderId={} productId={} error={}",
                            event.orderId(), line.productId(), result.getError());
                    throw new IllegalStateException(
                            "Cannot create subscription for product " + line.productId()
                                    + " on order " + event.orderId() + ": " + result.getError());
                }
            }
        });
    }

    private Instant calculateEndDate(Instant startAt, BillingCycle billingCycle) {
        LocalDate startDate = startAt.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = billingCycle == BillingCycle.MONTHLY
                ? startDate.plusMonths(1)
                : startDate.plusYears(1);
        return endDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
