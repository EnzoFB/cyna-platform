package com.cyna.modules.payment.application.command.finalize;

import com.cyna.modules.order.application.api.OrderCommandApi;
import com.cyna.modules.order.application.api.OrderPaymentView;
import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.application.model.PaymentFinalizedReadModel;
import com.cyna.modules.payment.domain.model.Payment;
import com.cyna.modules.payment.domain.model.PaymentStatus;
import com.cyna.modules.payment.domain.model.OrderTaxSnapshot;
import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.OrderTaxSnapshotRepository;
import com.cyna.modules.payment.domain.repository.PaymentRepository;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.modules.subscription.application.api.SubscriptionCommandApi;
import com.cyna.modules.subscription.application.api.SubscriptionCommandApi.CreatedSubscriptionView;
import com.cyna.modules.subscription.application.api.SubscriptionQueryApi;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalizePaymentCommandHandlerTest {

    @Mock
    private OrderQueryApi orderQueryApi;
    @Mock
    private OrderCommandApi orderCommandApi;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private StripeCustomerRepository stripeCustomerRepository;
    @Mock
    private PaymentGatewayPort paymentGateway;
    @Mock
    private SubscriptionCommandApi subscriptionCommandApi;
    @Mock
    private SubscriptionQueryApi subscriptionQueryApi;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private OrderTaxSnapshotRepository orderTaxSnapshotRepository;

    private FinalizePaymentCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) { action.run(); }

        @Override
        public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new FinalizePaymentCommandHandler(
                orderQueryApi, orderCommandApi, paymentRepository,
                stripeCustomerRepository, paymentGateway,
                subscriptionCommandApi, subscriptionQueryApi, eventPublisher, transactionRunner,
                orderTaxSnapshotRepository
        );
    }

    @Test
    void should_fail_when_order_not_found() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.empty());

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_1", null, "fr"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("ORDER_NOT_FOUND");
        verify(paymentGateway, never()).createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                any(), any(), anyInt());
    }

    @Test
    void should_decline_and_roll_back_when_the_single_line_is_not_settled() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        OrderPaymentView order = orderWith(orderId, userId, lineId);
        Payment pending = pendingPayment(orderId, userId);

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(pending));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any(), anyInt()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "incomplete", null, null, null));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_card_declined", null, "fr"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("PAYMENT_DECLINED");

        // Stripe rollback: the incomplete subscription is cancelled immediately.
        verify(paymentGateway).cancelSubscriptionNow("sub_1");
        // No order confirmation, no local subscription created.
        verify(orderCommandApi, never()).markOrderAsPaid(any(), any());
        verify(subscriptionCommandApi, never()).createFromPayment(any());
        // Payment marked FAILED (retryable) and the PaymentFailed event published.
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(eventPublisher).publishAll(any());
    }

    @Test
    void should_roll_back_every_line_when_only_one_of_several_is_not_settled() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lineA = UUID.randomUUID();
        UUID lineB = UUID.randomUUID();

        OrderPaymentView order = new OrderPaymentView(
                orderId, userId, "PENDING", BigDecimal.valueOf(300), "EUR",
                List.of(
                        new OrderPaymentView.OrderLineView(
                                lineA, UUID.randomUUID(), "SOC", "SOC", "MONTHLY", 1, BigDecimal.valueOf(100), 0),
                        new OrderPaymentView.OrderLineView(
                                lineB, UUID.randomUUID(), "EDR", "EDR", "ANNUAL", 1, BigDecimal.valueOf(200), 0)
                ));
        Payment pending = pendingPayment(orderId, userId);

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(pending));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(lineA), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any(), anyInt()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_A", "active", null, null, null));
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(lineB), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any(), anyInt()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_B", "incomplete", null, null, null));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_x", null, "fr"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("PAYMENT_DECLINED");
        // Atomic order: even the line that settled is rolled back.
        verify(paymentGateway).cancelSubscriptionNow("sub_A");
        verify(paymentGateway).cancelSubscriptionNow("sub_B");
        verify(orderCommandApi, never()).markOrderAsPaid(any(), any());
        verify(subscriptionCommandApi, never()).createFromPayment(any());
    }

    @Test
    void should_not_remark_failed_when_payment_was_already_failed_on_a_retry_decline() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        OrderPaymentView order = orderWith(orderId, userId, lineId);
        Payment alreadyFailed = pendingPayment(orderId, userId).markFailed().getValue();

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(alreadyFailed));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any(), anyInt()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "incomplete", null, null, null));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_card_declined_again", null, "fr"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("PAYMENT_DECLINED");
        verify(paymentGateway).cancelSubscriptionNow("sub_1");
        // Payment is already FAILED — no re-mark, no duplicate event.
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishAll(any());
    }

    @Test
    void should_succeed_and_confirm_order_when_all_lines_are_active() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        OrderPaymentView order = orderWith(orderId, userId, lineId);
        Payment pending = pendingPayment(orderId, userId);

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(pending));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any(), anyInt()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "active", null, null, null));
        UUID localSubId = UUID.randomUUID();
        when(subscriptionCommandApi.createFromPayment(any()))
                .thenReturn(Result.success(subReadModel(localSubId)));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_ok", null, "fr"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().lines()).hasSize(1);
        assertThat(result.getValue().lines().get(0).stripeStatus()).isEqualTo("active");
        verify(orderCommandApi).markOrderAsPaid(eq(orderId), any());
        verify(paymentGateway, never()).cancelSubscriptionNow(any());
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void should_grant_the_free_trial_on_a_first_time_subscription() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderPaymentView order = orderWithTrial(orderId, userId, lineId, productId, 14);
        Payment pending = pendingPayment(orderId, userId);

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(pending));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        // First time this customer subscribes to the product → trial eligible.
        when(subscriptionQueryApi.hasEverSubscribed(userId, productId)).thenReturn(false);
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), any(), any(), anyInt()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "trialing", null, null, null));
        when(subscriptionCommandApi.createFromPayment(any()))
                .thenReturn(Result.success(subReadModel(UUID.randomUUID())));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_ok", null, "fr"));

        assertThat(result.isSuccess()).isTrue();
        // The product's 14-day trial is passed through to Stripe.
        ArgumentCaptor<Integer> trial = ArgumentCaptor.forClass(Integer.class);
        verify(paymentGateway).createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), any(), any(), trial.capture());
        assertThat(trial.getValue()).isEqualTo(14);
    }

    @Test
    void should_suppress_the_free_trial_when_the_customer_already_subscribed_to_the_product() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderPaymentView order = orderWithTrial(orderId, userId, lineId, productId, 14);
        Payment pending = pendingPayment(orderId, userId);

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(pending));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        // Returning customer already had this product → no second trial.
        when(subscriptionQueryApi.hasEverSubscribed(userId, productId)).thenReturn(true);
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), any(), any(), anyInt()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "active", null, null, null));
        when(subscriptionCommandApi.createFromPayment(any()))
                .thenReturn(Result.success(subReadModel(UUID.randomUUID())));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_ok", null, "fr"));

        assertThat(result.isSuccess()).isTrue();
        // Charged immediately: trial days forced to 0 despite the product advertising one.
        ArgumentCaptor<Integer> trial = ArgumentCaptor.forClass(Integer.class);
        verify(paymentGateway).createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), any(), any(), trial.capture());
        assertThat(trial.getValue()).isZero();
    }

    @Test
    void should_allow_a_retry_to_succeed_after_a_previous_decline() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        OrderPaymentView order = orderWith(orderId, userId, lineId);
        // Payment is FAILED from a first declined attempt; the customer retries
        // with a good card. The relaxed guard must let this through.
        Payment failed = pendingPayment(orderId, userId).markFailed().getValue();

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(failed));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any(), anyInt()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "active", null, null, null));
        when(subscriptionCommandApi.createFromPayment(any()))
                .thenReturn(Result.success(subReadModel(UUID.randomUUID())));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_good_card", null, "fr"));

        assertThat(result.isSuccess()).isTrue();
        verify(orderCommandApi).markOrderAsPaid(eq(orderId), any());
    }

    @Test
    void should_pin_customer_tax_location_before_creating_any_subscription() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        OrderPaymentView order = orderWith(orderId, userId, lineId);
        Payment pending = pendingPayment(orderId, userId);

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(pending));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "active", null, null, null));
        when(subscriptionCommandApi.createFromPayment(any()))
                .thenReturn(Result.success(subReadModel(UUID.randomUUID())));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_eu", "FR12345678901", "fr"));

        assertThat(result.isSuccess()).isTrue();
        // Tax jurisdiction (address + B2B VAT number) MUST be pinned before the
        // first charge, otherwise Stripe Tax computes VAT against a stale/absent
        // customer address and never applies the reverse charge.
        InOrder inOrder = inOrder(paymentGateway);
        inOrder.verify(paymentGateway).updateCustomerTaxLocation("cus_x", "pm_eu", "FR12345678901");
        inOrder.verify(paymentGateway).createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), anyInt());
    }

    @Test
    void should_fail_closed_with_stripe_error_when_tax_location_sync_fails() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        OrderPaymentView order = orderWith(orderId, userId, lineId);
        Payment pending = pendingPayment(orderId, userId);

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(pending));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        doThrow(new PaymentGatewayException("tax location update failed", null))
                .when(paymentGateway).updateCustomerTaxLocation(eq("cus_x"), any(), any());

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_x", null, "fr"));

        // Never charge an untaxed/incorrectly-taxed amount: no subscription,
        // no order paid.
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).startsWith("STRIPE_ERROR");
        verify(paymentGateway, never()).createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), anyInt());
        verify(orderCommandApi, never()).markOrderAsPaid(any(), any());
    }

    @Test
    void should_capture_order_tax_snapshot_from_the_checkout_invoice() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        OrderPaymentView order = orderWith(orderId, userId, lineId);
        Payment pending = pendingPayment(orderId, userId);

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(pending));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        // Stripe billed 120.00 TTC = 100.00 HT + 20.00 VAT on the checkout invoice.
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult(
                        "sub_1", "active", null, "succeeded", null,
                        12000L, 2000L, false, "EUR"));
        when(subscriptionCommandApi.createFromPayment(any()))
                .thenReturn(Result.success(subReadModel(UUID.randomUUID())));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_ok", null, "fr"));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<OrderTaxSnapshot> snapshot = ArgumentCaptor.forClass(OrderTaxSnapshot.class);
        verify(orderTaxSnapshotRepository).save(snapshot.capture());
        OrderTaxSnapshot s = snapshot.getValue();
        assertThat(s.orderId()).isEqualTo(orderId);
        assertThat(s.userId()).isEqualTo(userId);
        assertThat(s.subtotalHt()).isEqualByComparingTo("100.00");
        assertThat(s.vatAmount()).isEqualByComparingTo("20.00");
        assertThat(s.totalTtc()).isEqualByComparingTo("120.00");
        assertThat(s.currency()).isEqualTo("EUR");
        assertThat(s.reverseCharge()).isFalse();
    }

    @Test
    void should_not_capture_a_snapshot_when_no_line_exposes_invoice_tax() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        OrderPaymentView order = orderWith(orderId, userId, lineId);
        Payment pending = pendingPayment(orderId, userId);

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(pending));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        // No invoice figures surfaced (compat result) → nothing to snapshot; the
        // read path will fall back to a live Stripe read / HT subtotal.
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "active", null, null, null));
        when(subscriptionCommandApi.createFromPayment(any()))
                .thenReturn(Result.success(subReadModel(UUID.randomUUID())));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_ok", null, "fr"));

        assertThat(result.isSuccess()).isTrue();
        verify(orderTaxSnapshotRepository, never()).save(any());
    }

    private OrderPaymentView orderWith(UUID orderId, UUID userId, UUID lineId) {
        return new OrderPaymentView(
                orderId, userId, "PENDING", BigDecimal.valueOf(100), "EUR",
                List.of(new OrderPaymentView.OrderLineView(
                        lineId, UUID.randomUUID(), "SOC", "SOC", "MONTHLY", 1, BigDecimal.valueOf(100), 0)));
    }

    private OrderPaymentView orderWithTrial(UUID orderId, UUID userId, UUID lineId,
                                            UUID productId, int trialDays) {
        return new OrderPaymentView(
                orderId, userId, "PENDING", BigDecimal.valueOf(100), "EUR",
                List.of(new OrderPaymentView.OrderLineView(
                        lineId, productId, "SOC", "SOC", "MONTHLY", 1, BigDecimal.valueOf(100), trialDays)));
    }

    private Payment pendingPayment(UUID orderId, UUID userId) {
        return Payment.create(UUID.randomUUID(), orderId, userId,
                Money.of(BigDecimal.valueOf(100), "EUR"));
    }

    private CreatedSubscriptionView subReadModel(UUID id) {
        return new CreatedSubscriptionView(id);
    }
}
