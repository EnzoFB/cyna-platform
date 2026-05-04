package com.cyna.modules.payment.infrastructure.event;

import com.cyna.modules.order.application.api.OrderCommandApi;
import com.cyna.modules.order.application.api.OrderPaymentView;
import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.domain.event.PaymentSucceeded;
import com.cyna.modules.subscription.application.api.SubscriptionCommandApi;
import com.cyna.modules.subscription.application.api.SubscriptionPaymentPayload;
import com.cyna.modules.subscription.domain.model.BillingCycle;
import com.cyna.shared.application.TransactionRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Reacts to a successful payment by marking the related order as PAID and
 * activating one local Subscription per order line.
 *
 * <p><b>Atomicity guarantee.</b> The two side-effects (mark order paid, create
 * subscriptions) MUST commit together with the Payment SUCCEEDED state — or
 * roll back together. We achieve this by:
 * <ol>
 *   <li>Running as a synchronous {@link EventListener}, so the listener
 *       executes in the publisher's thread, joining the publisher's
 *       transaction (Spring's {@code TransactionTemplate} uses
 *       {@code PROPAGATION_REQUIRED}).</li>
 *   <li>Wrapping the body in {@link TransactionRunner#run} explicitly to make
 *       the atomic intent visible and resilient to refactors (e.g. if someone
 *       later adds {@code @Async} or switches to {@code AFTER_COMMIT}, the two
 *       inner operations remain atomic between themselves).</li>
 * </ol>
 *
 * <p>If any inner step fails we throw, the whole transaction rolls back, and
 * Stripe receives a 5xx — Stripe will retry the webhook (smart retries) which
 * is safe because every step is idempotent (Payment SUCCEEDED is detected
 * early, order PAID transition is rejected if already PAID, etc.).
 */
@Component
public class OnPaymentSucceededHandler {

    private static final Logger log = LoggerFactory.getLogger(OnPaymentSucceededHandler.class);

    private final OrderCommandApi orderCommandApi;
    private final OrderQueryApi orderQueryApi;
    private final SubscriptionCommandApi subscriptionCommandApi;
    private final TransactionRunner transactionRunner;

    public OnPaymentSucceededHandler(OrderCommandApi orderCommandApi,
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
            markOrderAsPaidOrThrow(event);
            createSubscriptionsOrThrow(event);
        });
    }

    private void markOrderAsPaidOrThrow(PaymentSucceeded event) {
        var result = orderCommandApi.markOrderAsPaid(event.orderId());
        if (result.isFailure()) {
            log.error("[payment-succeeded] mark order PAID failed — orderId={} userId={} paymentId={} error={}",
                    event.orderId(), event.userId(), event.paymentId(), result.getError());
            throw new IllegalStateException(
                    "Cannot mark order " + event.orderId() + " as paid: " + result.getError());
        }
    }

    private void createSubscriptionsOrThrow(PaymentSucceeded event) {
        OrderPaymentView order = orderQueryApi
                .findOrderForPayment(event.orderId(), event.userId())
                .orElse(null);

        if (order == null) {
            log.error("[payment-succeeded] order not found after mark-paid — orderId={} userId={}",
                    event.orderId(), event.userId());
            throw new IllegalStateException(
                    "Order " + event.orderId() + " disappeared after mark-paid");
        }

        Instant startAt = event.occurredAt();

        for (OrderPaymentView.OrderLineView line : order.lines()) {
            BillingCycle billingCycle = BillingCycle.valueOf(line.billingCycle());
            Instant endAt = calculateEndDate(startAt, billingCycle);

            var payload = new SubscriptionPaymentPayload(
                    event.userId(),
                    event.orderId(),
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
                    event.stripeSubscriptionId(),
                    null
            );

            var result = subscriptionCommandApi.createFromPayment(payload);
            if (result.isFailure()) {
                log.error("[payment-succeeded] create subscription failed — orderId={} productId={} error={}",
                        event.orderId(), line.productId(), result.getError());
                throw new IllegalStateException(
                        "Cannot create subscription for product " + line.productId()
                                + " on order " + event.orderId() + ": " + result.getError());
            }
        }
    }

    private Instant calculateEndDate(Instant startAt, BillingCycle billingCycle) {
        LocalDate startDate = startAt.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = billingCycle == BillingCycle.MONTHLY
                ? startDate.plusMonths(1)
                : startDate.plusYears(1);
        return endDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
