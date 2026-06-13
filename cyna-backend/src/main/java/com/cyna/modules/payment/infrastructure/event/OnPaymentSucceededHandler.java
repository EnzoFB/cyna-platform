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
 * Legacy provisioning path: reacts to {@link PaymentSucceeded} from the pre-V14
 * single-Stripe-sub-per-order flow. The new (V14+) multi-product checkout
 * provisions everything eagerly inside {@code FinalizePaymentCommandHandler}
 * and the same {@link PaymentSucceeded} event is fired afterwards — so this
 * handler MUST be idempotent. We detect "already provisioned" by checking the
 * Order status (set to PAID earlier in the same transaction by finalize) and
 * short-circuit without retrying.
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
            OrderPaymentView order = orderQueryApi
                    .findOrderForPayment(event.orderId(), event.userId())
                    .orElse(null);

            if (order == null) {
                log.error("[payment-succeeded] order not found — orderId={} userId={}",
                        event.orderId(), event.userId());
                throw new IllegalStateException(
                        "Order " + event.orderId() + " not found");
            }

            // Already provisioned (new V14+ flow ran in finalize → marked PAID). Skip.
            if ("PAID".equals(order.status()) || "FULFILLED".equals(order.status())) {
                return;
            }

            // Legacy flow: mark PAID and create one Subscription per OrderLine using the
            // single shared Stripe Subscription id carried by the PaymentSucceeded event.
            Result<Void> paidResult = orderCommandApi.markOrderAsPaid(event.orderId());
            if (paidResult.isFailure()) {
                log.error("[payment-succeeded] mark order PAID failed — orderId={} userId={} error={}",
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
                        null, // legacy flow: pre-V14 payments don't carry order_line_id
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

                Result<?> result = subscriptionCommandApi.createFromPayment(payload);
                if (result.isFailure()) {
                    log.error("[payment-succeeded] create subscription failed — orderId={} productId={} error={}",
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
