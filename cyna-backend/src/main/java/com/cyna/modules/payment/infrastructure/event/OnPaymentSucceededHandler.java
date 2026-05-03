package com.cyna.modules.payment.infrastructure.event;

import com.cyna.modules.order.application.api.OrderCommandApi;
import com.cyna.modules.order.application.api.OrderPaymentView;
import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.domain.event.PaymentSucceeded;
import com.cyna.modules.subscription.application.api.SubscriptionCommandApi;
import com.cyna.modules.subscription.application.api.SubscriptionPaymentPayload;
import com.cyna.modules.subscription.domain.model.BillingCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class OnPaymentSucceededHandler {

    private static final Logger log = LoggerFactory.getLogger(OnPaymentSucceededHandler.class);

    private final OrderCommandApi orderCommandApi;
    private final OrderQueryApi orderQueryApi;
    private final SubscriptionCommandApi subscriptionCommandApi;

    public OnPaymentSucceededHandler(OrderCommandApi orderCommandApi,
                                     OrderQueryApi orderQueryApi,
                                     SubscriptionCommandApi subscriptionCommandApi) {
        this.orderCommandApi = orderCommandApi;
        this.orderQueryApi = orderQueryApi;
        this.subscriptionCommandApi = subscriptionCommandApi;
    }

    @EventListener
    public void on(PaymentSucceeded event) {
        // 1. Marquer la commande comme payée
        var payResult = orderCommandApi.markOrderAsPaid(event.orderId());
        if (payResult.isFailure()) {
            log.error("Failed to mark order {} as paid: {}", event.orderId(), payResult.getError());
            throw new IllegalStateException("Cannot mark order as paid: " + payResult.getError());
        }

        // 2. Charger les lignes de commande pour créer les abonnements
        OrderPaymentView order = orderQueryApi
                .findOrderForPayment(event.orderId(), event.userId())
                .orElse(null);

        if (order == null) {
            log.error("Order {} not found after marking as paid", event.orderId());
            return;
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
                log.error("Failed to create subscription for product {} on order {}: {}",
                        line.productId(), event.orderId(), result.getError());
                throw new IllegalStateException("Cannot create subscription: " + result.getError());
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
