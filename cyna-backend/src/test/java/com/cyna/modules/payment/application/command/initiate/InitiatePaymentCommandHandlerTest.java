package com.cyna.modules.payment.application.command.initiate;

import com.cyna.modules.order.application.api.OrderPaymentView;
import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.application.model.PaymentInitiatedReadModel;
import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.PaymentRepository;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.modules.user.application.api.UserPaymentView;
import com.cyna.modules.user.application.api.UserQueryApi;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiatePaymentCommandHandlerTest {

    @Mock
    private OrderQueryApi orderQueryApi;
    @Mock
    private UserQueryApi userQueryApi;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private StripeCustomerRepository stripeCustomerRepository;
    @Mock
    private PaymentGatewayPort paymentGateway;
    @Mock
    private DomainEventPublisher eventPublisher;

    private InitiatePaymentCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) { action.run(); }

        @Override
        public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new InitiatePaymentCommandHandler(
                orderQueryApi, userQueryApi, paymentRepository,
                stripeCustomerRepository, paymentGateway,
                eventPublisher, transactionRunner
        );
    }

    @Test
    void should_fail_when_order_not_found() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.empty());

        Result<PaymentInitiatedReadModel> result =
                handler.handle(new InitiatePaymentCommand(orderId, userId));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("ORDER_NOT_FOUND");
        verify(paymentGateway, never()).createSubscription(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_fail_when_order_not_pending() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderPaymentView order = orderViewWithStatus(orderId, userId, "PAID", "MONTHLY");
        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));

        Result<PaymentInitiatedReadModel> result =
                handler.handle(new InitiatePaymentCommand(orderId, userId));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("ORDER_NOT_PAYABLE");
    }

    @Test
    void should_fail_when_order_lines_have_mixed_billing_cycles() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderPaymentView order = new OrderPaymentView(
                orderId, userId, "PENDING", BigDecimal.valueOf(300), "EUR",
                List.of(
                        new OrderPaymentView.OrderLineView(
                                UUID.randomUUID(), "SOC", "SOC", "MONTHLY", 1, BigDecimal.valueOf(100)
                        ),
                        new OrderPaymentView.OrderLineView(
                                UUID.randomUUID(), "EDR", "EDR", "ANNUAL", 1, BigDecimal.valueOf(200)
                        )
                )
        );
        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));

        Result<PaymentInitiatedReadModel> result =
                handler.handle(new InitiatePaymentCommand(orderId, userId));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("MIXED_BILLING_CYCLES");
        verify(paymentGateway, never()).createSubscription(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_fail_when_user_not_found() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(orderQueryApi.findOrderForPayment(orderId, userId))
                .thenReturn(Optional.of(orderViewWithStatus(orderId, userId, "PENDING", "MONTHLY")));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(userQueryApi.findUserForPayment(userId)).thenReturn(Optional.empty());

        Result<PaymentInitiatedReadModel> result =
                handler.handle(new InitiatePaymentCommand(orderId, userId));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void should_create_subscription_and_persist_payment_on_happy_path() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderPaymentView order = orderViewWithStatus(orderId, userId, "PENDING", "MONTHLY");

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(userQueryApi.findUserForPayment(userId)).thenReturn(Optional.of(
                new UserPaymentView(userId, "a@b.com", "Jean", "Dupont")
        ));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.empty());
        when(paymentGateway.createSubscription(
                eq(orderId), any(), eq("a@b.com"), eq("Jean Dupont"),
                any(), eq("EUR"), eq("MONTHLY")
        )).thenReturn(new PaymentGatewayPort.SubscriptionResult(
                "cus_123", "sub_123", "sch_123", "pi_secret_abc", "pi_123"
        ));

        Result<PaymentInitiatedReadModel> result =
                handler.handle(new InitiatePaymentCommand(orderId, userId));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().clientSecret()).isEqualTo("pi_secret_abc");
        assertThat(result.getValue().orderId()).isEqualTo(orderId);

        verify(stripeCustomerRepository).save(userId, "cus_123");
        verify(paymentRepository).save(any());
        verify(eventPublisher).publishAll(any());
    }

    @Test
    void should_be_idempotent_when_pending_payment_already_exists() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderPaymentView order = orderViewWithStatus(orderId, userId, "PENDING", "MONTHLY");

        var existingPayment = com.cyna.modules.payment.domain.model.Payment.create(
                UUID.randomUUID(), orderId, userId,
                com.cyna.shared.domain.Money.of(BigDecimal.valueOf(100), "EUR")
        ).assignStripeSubscription("pi_old", "pi_old_secret", "sub_old", "sch_old");

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingPayment));

        Result<PaymentInitiatedReadModel> result =
                handler.handle(new InitiatePaymentCommand(orderId, userId));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().clientSecret()).isEqualTo("pi_old_secret");
        verify(paymentGateway, never()).createSubscription(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_return_stripe_error_when_gateway_fails() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderPaymentView order = orderViewWithStatus(orderId, userId, "PENDING", "MONTHLY");

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(userQueryApi.findUserForPayment(userId)).thenReturn(Optional.of(
                new UserPaymentView(userId, "a@b.com", "Jean", "Dupont")
        ));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.empty());
        when(paymentGateway.createSubscription(
                any(), any(), any(), any(), any(), any(), any()
        )).thenThrow(new PaymentGatewayException("boom", null));

        Result<PaymentInitiatedReadModel> result =
                handler.handle(new InitiatePaymentCommand(orderId, userId));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).startsWith("STRIPE_ERROR");
    }

    @Test
    void should_upsert_stripe_customer_when_gateway_returns_new_customer_id() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderPaymentView order = orderViewWithStatus(orderId, userId, "PENDING", "MONTHLY");

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(userQueryApi.findUserForPayment(userId)).thenReturn(Optional.of(
                new UserPaymentView(userId, "a@b.com", "Jean", "Dupont")
        ));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_stale"));
        // adapter fell back to a fresh customer
        when(paymentGateway.createSubscription(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaymentGatewayPort.SubscriptionResult(
                        "cus_fresh", "sub_1", "sch_1", "pi_secret", "pi_1"
                ));

        handler.handle(new InitiatePaymentCommand(orderId, userId));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(stripeCustomerRepository).save(eq(userId), captor.capture());
        assertThat(captor.getValue()).isEqualTo("cus_fresh");
    }

    private OrderPaymentView orderViewWithStatus(UUID orderId, UUID userId, String status, String cycle) {
        return new OrderPaymentView(
                orderId, userId, status, BigDecimal.valueOf(100), "EUR",
                List.of(new OrderPaymentView.OrderLineView(
                        UUID.randomUUID(), "SOC", "SOC", cycle, 1, BigDecimal.valueOf(100)
                ))
        );
    }
}
