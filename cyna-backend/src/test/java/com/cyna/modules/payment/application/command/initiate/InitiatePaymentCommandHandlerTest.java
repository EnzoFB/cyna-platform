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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        verify(paymentGateway, never()).createSetupIntent(any());
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
    void should_accept_orders_with_mixed_billing_cycles() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderPaymentView order = new OrderPaymentView(
                orderId, userId, "PENDING", BigDecimal.valueOf(300), "EUR",
                List.of(
                        new OrderPaymentView.OrderLineView(
                                UUID.randomUUID(), UUID.randomUUID(), "SOC", "SOC", "MONTHLY", 1, BigDecimal.valueOf(100), 0
                        ),
                        new OrderPaymentView.OrderLineView(
                                UUID.randomUUID(), UUID.randomUUID(), "EDR", "EDR", "ANNUAL", 1, BigDecimal.valueOf(200), 0
                        )
                )
        );
        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(userQueryApi.findUserForPayment(userId)).thenReturn(Optional.of(
                new UserPaymentView(userId, "a@b.com", "Jean", "Dupont")
        ));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        when(paymentGateway.createSetupIntent("cus_x"))
                .thenReturn(new PaymentGatewayPort.SetupIntentResult("seti_1", "seti_secret"));

        Result<PaymentInitiatedReadModel> result =
                handler.handle(new InitiatePaymentCommand(orderId, userId));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().setupIntentClientSecret()).isEqualTo("seti_secret");
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
    void should_create_setup_intent_and_persist_payment_on_happy_path() {
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
        when(paymentGateway.createCustomerForUser(userId, "a@b.com", "Jean Dupont"))
                .thenReturn("cus_new");
        when(paymentGateway.createSetupIntent("cus_new"))
                .thenReturn(new PaymentGatewayPort.SetupIntentResult("seti_abc", "seti_secret_abc"));

        Result<PaymentInitiatedReadModel> result =
                handler.handle(new InitiatePaymentCommand(orderId, userId));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().setupIntentClientSecret()).isEqualTo("seti_secret_abc");
        assertThat(result.getValue().orderId()).isEqualTo(orderId);

        verify(stripeCustomerRepository).save(userId, "cus_new");
        verify(paymentRepository).save(any());
        verify(eventPublisher).publishAll(any());
    }

    @Test
    void should_be_idempotent_when_pending_payment_with_setup_intent_already_exists() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderPaymentView order = orderViewWithStatus(orderId, userId, "PENDING", "MONTHLY");

        var existingPayment = com.cyna.modules.payment.domain.model.Payment.create(
                UUID.randomUUID(), orderId, userId,
                com.cyna.shared.domain.Money.of(BigDecimal.valueOf(100), "EUR")
        ).assignSetupIntent("seti_old", "seti_old_secret");

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingPayment));

        Result<PaymentInitiatedReadModel> result =
                handler.handle(new InitiatePaymentCommand(orderId, userId));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().setupIntentClientSecret()).isEqualTo("seti_old_secret");
        verify(paymentGateway, never()).createSetupIntent(any());
    }

    @Test
    void should_return_stripe_error_when_setup_intent_creation_fails() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderPaymentView order = orderViewWithStatus(orderId, userId, "PENDING", "MONTHLY");

        when(orderQueryApi.findOrderForPayment(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(userQueryApi.findUserForPayment(userId)).thenReturn(Optional.of(
                new UserPaymentView(userId, "a@b.com", "Jean", "Dupont")
        ));
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_x"));
        when(paymentGateway.createSetupIntent("cus_x"))
                .thenThrow(new PaymentGatewayException("boom", null));

        Result<PaymentInitiatedReadModel> result =
                handler.handle(new InitiatePaymentCommand(orderId, userId));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).startsWith("STRIPE_ERROR");
    }

    private OrderPaymentView orderViewWithStatus(UUID orderId, UUID userId, String status, String cycle) {
        return new OrderPaymentView(
                orderId, userId, status, BigDecimal.valueOf(100), "EUR",
                List.of(new OrderPaymentView.OrderLineView(
                        UUID.randomUUID(), UUID.randomUUID(), "SOC", "SOC", cycle, 1, BigDecimal.valueOf(100), 0
                ))
        );
    }
}
