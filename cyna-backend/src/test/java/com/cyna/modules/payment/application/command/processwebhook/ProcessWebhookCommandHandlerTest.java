package com.cyna.modules.payment.application.command.processwebhook;

import com.cyna.modules.payment.application.command.processresult.ProcessPaymentResultCommand;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.port.WebhookSignatureException;
import com.cyna.modules.subscription.application.api.SubscriptionCommandApi;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessWebhookCommandHandlerTest {

    @Mock
    private PaymentGatewayPort paymentGateway;

    @Mock
    private Mediator mediator;

    @Mock
    private SubscriptionCommandApi subscriptionCommandApi;

    private ProcessWebhookCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProcessWebhookCommandHandler(paymentGateway, mediator, subscriptionCommandApi);
    }

    @Test
    void should_reject_invalid_signature() {
        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenThrow(new WebhookSignatureException("bad", null));

        Result<Void> result = handler.handle(new ProcessWebhookCommand("payload", "sig"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("INVALID_WEBHOOK_SIGNATURE");
        verify(mediator, never()).send(any(ProcessPaymentResultCommand.class));
        verify(subscriptionCommandApi, never()).renewByStripeId(anyString(), any());
    }

    @Test
    void should_route_first_invoice_paid_to_payment_success_command() {
        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(stripeEvent("invoice.paid", "pi_123", "sub_123",
                        "subscription_create", null));
        when(mediator.send(any(ProcessPaymentResultCommand.class))).thenReturn(Result.success());

        Result<Void> result = handler.handle(new ProcessWebhookCommand("payload", "sig"));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<ProcessPaymentResultCommand> cmd =
                ArgumentCaptor.forClass(ProcessPaymentResultCommand.class);
        verify(mediator).send(cmd.capture());
        assertThat(cmd.getValue().stripePaymentIntentId()).isEqualTo("pi_123");
        assertThat(cmd.getValue().succeeded()).isTrue();
        verify(subscriptionCommandApi, never()).renewByStripeId(anyString(), any());
    }

    @Test
    void should_route_renewal_invoice_paid_to_subscription_renew_api() {
        Instant newPeriodEnd = Instant.parse("2026-05-29T00:00:00Z");
        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(stripeEvent("invoice.paid", "pi_999", "sub_123",
                        "subscription_cycle", newPeriodEnd));
        when(subscriptionCommandApi.renewByStripeId(eq("sub_123"), eq(newPeriodEnd)))
                .thenReturn(Result.success());

        Result<Void> result = handler.handle(new ProcessWebhookCommand("payload", "sig"));

        assertThat(result.isSuccess()).isTrue();
        verify(subscriptionCommandApi).renewByStripeId("sub_123", newPeriodEnd);
        verify(mediator, never()).send(any(ProcessPaymentResultCommand.class));
    }

    @Test
    void should_ignore_renewal_invoice_paid_when_period_end_missing() {
        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(stripeEvent("invoice.paid", "pi_999", "sub_123",
                        "subscription_cycle", null));

        Result<Void> result = handler.handle(new ProcessWebhookCommand("payload", "sig"));

        assertThat(result.isSuccess()).isTrue();
        verify(subscriptionCommandApi, never()).renewByStripeId(anyString(), any());
    }

    @Test
    void should_route_first_invoice_payment_failed_to_payment_failure_command() {
        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(stripeEvent("invoice.payment_failed", "pi_456", "sub_456",
                        "subscription_create", null));
        when(mediator.send(any(ProcessPaymentResultCommand.class))).thenReturn(Result.success());

        Result<Void> result = handler.handle(new ProcessWebhookCommand("payload", "sig"));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<ProcessPaymentResultCommand> cmd =
                ArgumentCaptor.forClass(ProcessPaymentResultCommand.class);
        verify(mediator).send(cmd.capture());
        assertThat(cmd.getValue().stripePaymentIntentId()).isEqualTo("pi_456");
        assertThat(cmd.getValue().succeeded()).isFalse();
        verify(subscriptionCommandApi, never()).markPastDueByStripeId(anyString());
    }

    @Test
    void should_route_renewal_invoice_payment_failed_to_mark_past_due() {
        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(stripeEvent("invoice.payment_failed", "pi_789", "sub_789",
                        "subscription_cycle", null));
        when(subscriptionCommandApi.markPastDueByStripeId("sub_789"))
                .thenReturn(Result.success());

        Result<Void> result = handler.handle(new ProcessWebhookCommand("payload", "sig"));

        assertThat(result.isSuccess()).isTrue();
        verify(subscriptionCommandApi).markPastDueByStripeId("sub_789");
        verify(mediator, never()).send(any(ProcessPaymentResultCommand.class));
    }

    @Test
    void should_route_subscription_deleted_to_cancel_api() {
        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(new PaymentGatewayPort.StripeWebhookEvent(
                        "customer.subscription.deleted", null, "sub_abc", "cus_abc",
                        null, null, null
                ));
        when(subscriptionCommandApi.cancelByStripeId("sub_abc"))
                .thenReturn(Result.success());

        Result<Void> result = handler.handle(new ProcessWebhookCommand("payload", "sig"));

        assertThat(result.isSuccess()).isTrue();
        verify(subscriptionCommandApi).cancelByStripeId("sub_abc");
    }

    @Test
    void should_ignore_unknown_event_types() {
        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(new PaymentGatewayPort.StripeWebhookEvent(
                        "customer.subscription.updated", null, "sub_x", "cus_x",
                        null, null, null
                ));

        Result<Void> result = handler.handle(new ProcessWebhookCommand("payload", "sig"));

        assertThat(result.isSuccess()).isTrue();
        verify(mediator, never()).send(any(ProcessPaymentResultCommand.class));
        verify(subscriptionCommandApi, never()).renewByStripeId(anyString(), any());
    }

    @Test
    void should_route_legacy_payment_intent_succeeded() {
        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(new PaymentGatewayPort.StripeWebhookEvent(
                        "payment_intent.succeeded", "pi_legacy", null, "cus_legacy",
                        null, null, null
                ));
        when(mediator.send(any(ProcessPaymentResultCommand.class))).thenReturn(Result.success());

        Result<Void> result = handler.handle(new ProcessWebhookCommand("payload", "sig"));

        assertThat(result.isSuccess()).isTrue();
        verify(mediator).send(any(ProcessPaymentResultCommand.class));
    }

    private PaymentGatewayPort.StripeWebhookEvent stripeEvent(
            String type, String paymentIntentId, String subscriptionId,
            String billingReason, Instant periodEnd) {
        return new PaymentGatewayPort.StripeWebhookEvent(
                type, paymentIntentId, subscriptionId, "cus_x",
                "in_x", billingReason, periodEnd
        );
    }
}
