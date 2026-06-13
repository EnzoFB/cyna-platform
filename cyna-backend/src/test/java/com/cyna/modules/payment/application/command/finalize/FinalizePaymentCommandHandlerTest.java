package com.cyna.modules.payment.application.command.finalize;

import com.cyna.modules.order.application.api.OrderCommandApi;
import com.cyna.modules.order.application.api.OrderPaymentView;
import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.application.model.PaymentFinalizedReadModel;
import com.cyna.modules.payment.domain.model.Payment;
import com.cyna.modules.payment.domain.model.PaymentStatus;
import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.PaymentRepository;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.modules.subscription.application.api.SubscriptionCommandApi;
import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
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
    private DomainEventPublisher eventPublisher;

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
                subscriptionCommandApi, eventPublisher, transactionRunner
        );
    }

    @Test
    void should_fail_when_order_not_found() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.empty());

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_1", null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("ORDER_NOT_FOUND");
        verify(paymentGateway, never()).createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                any(), any());
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
                any(), any(), any(), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "incomplete"));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_card_declined", null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("PAYMENT_DECLINED");

        // Stripe rollback: the incomplete subscription is cancelled immediately.
        verify(paymentGateway).cancelSubscriptionNow("sub_1");
        // No order confirmation, no local subscription created.
        verify(orderCommandApi, never()).markOrderAsPaid(any());
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
                                lineA, UUID.randomUUID(), "SOC", "SOC", "MONTHLY", 1, BigDecimal.valueOf(100)),
                        new OrderPaymentView.OrderLineView(
                                lineB, UUID.randomUUID(), "EDR", "EDR", "ANNUAL", 1, BigDecimal.valueOf(200))
                ));
        Payment pending = pendingPayment(orderId, userId);

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(pending));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), org.mockito.ArgumentMatchers.eq(lineA), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_A", "active"));
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), org.mockito.ArgumentMatchers.eq(lineB), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_B", "incomplete"));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_x", null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("PAYMENT_DECLINED");
        // Atomic order: even the line that settled is rolled back.
        verify(paymentGateway).cancelSubscriptionNow("sub_A");
        verify(paymentGateway).cancelSubscriptionNow("sub_B");
        verify(orderCommandApi, never()).markOrderAsPaid(any());
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
                any(), any(), any(), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "incomplete"));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_card_declined_again", null));

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
                any(), any(), any(), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "active"));
        UUID localSubId = UUID.randomUUID();
        when(subscriptionCommandApi.createFromPayment(any()))
                .thenReturn(Result.success(subReadModel(localSubId)));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_ok", null));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().lines()).hasSize(1);
        assertThat(result.getValue().lines().get(0).stripeStatus()).isEqualTo("active");
        verify(orderCommandApi).markOrderAsPaid(orderId);
        verify(paymentGateway, never()).cancelSubscriptionNow(any());
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
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
                any(), any(), any(), any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), any(), any()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "active"));
        when(subscriptionCommandApi.createFromPayment(any()))
                .thenReturn(Result.success(subReadModel(UUID.randomUUID())));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_good_card", null));

        assertThat(result.isSuccess()).isTrue();
        verify(orderCommandApi).markOrderAsPaid(orderId);
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
                any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(new PaymentGatewayPort.SubscriptionForLineResult("sub_1", "active"));
        when(subscriptionCommandApi.createFromPayment(any()))
                .thenReturn(Result.success(subReadModel(UUID.randomUUID())));

        Result<PaymentFinalizedReadModel> result =
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_eu", "FR12345678901"));

        assertThat(result.isSuccess()).isTrue();
        // Tax jurisdiction (address + B2B VAT number) MUST be pinned before the
        // first charge, otherwise Stripe Tax computes VAT against a stale/absent
        // customer address and never applies the reverse charge.
        InOrder inOrder = inOrder(paymentGateway);
        inOrder.verify(paymentGateway).updateCustomerTaxLocation("cus_x", "pm_eu", "FR12345678901");
        inOrder.verify(paymentGateway).createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
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
                handler.handle(new FinalizePaymentCommand(orderId, userId, "pm_x", null));

        // Never charge an untaxed/incorrectly-taxed amount: no subscription,
        // no order paid.
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).startsWith("STRIPE_ERROR");
        verify(paymentGateway, never()).createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(orderCommandApi, never()).markOrderAsPaid(any());
    }

    private OrderPaymentView orderWith(UUID orderId, UUID userId, UUID lineId) {
        return new OrderPaymentView(
                orderId, userId, "PENDING", BigDecimal.valueOf(100), "EUR",
                List.of(new OrderPaymentView.OrderLineView(
                        lineId, UUID.randomUUID(), "SOC", "SOC", "MONTHLY", 1, BigDecimal.valueOf(100))));
    }

    private Payment pendingPayment(UUID orderId, UUID userId) {
        return Payment.create(UUID.randomUUID(), orderId, userId,
                Money.of(BigDecimal.valueOf(100), "EUR"));
    }

    private SubscriptionReadModel subReadModel(UUID id) {
        return new SubscriptionReadModel(
                id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "SOC", "SOC", "MONTHLY", "ACTIVE", 1, BigDecimal.valueOf(100), "EUR",
                null, null, null, null, true, null, null, null);
    }
}
