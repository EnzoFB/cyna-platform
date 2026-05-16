package com.cyna.modules.payment.application.query.listpaymentmethods;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort.PaymentMethodSummary;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPaymentMethodsQueryHandlerTest {

    @Mock
    private StripeCustomerRepository stripeCustomerRepository;
    @Mock
    private PaymentGatewayPort paymentGateway;

    private ListPaymentMethodsQueryHandler handler;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListPaymentMethodsQueryHandler(stripeCustomerRepository, paymentGateway);
    }

    @Test
    void returns_cards_read_live_from_stripe_with_stripe_id_as_read_model_id() {
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_123"));
        when(paymentGateway.listPaymentMethods("cus_123")).thenReturn(List.of(
                new PaymentMethodSummary("pm_default", "Visa", "4242", "12", "2030", "Jane Doe", true),
                new PaymentMethodSummary("pm_other", "Mastercard", "4444", "01", "2029", "Jane Doe", false)
        ));

        List<SavedPaymentMethodReadModel> result = handler.handle(new ListPaymentMethodsQuery(userId));

        assertThat(result).hasSize(2);
        SavedPaymentMethodReadModel first = result.get(0);
        // The read-model id IS the Stripe PaymentMethod id — Stripe is the SoR,
        // the UI never diverges from the Stripe Portal.
        assertThat(first.id()).isEqualTo("pm_default");
        assertThat(first.stripePaymentMethodId()).isEqualTo("pm_default");
        assertThat(first.brand()).isEqualTo("Visa");
        assertThat(first.last4()).isEqualTo("4242");
        assertThat(first.expMonth()).isEqualTo("12");
        assertThat(first.expYear()).isEqualTo("2030");
        assertThat(first.holderName()).isEqualTo("Jane Doe");
        assertThat(first.isDefault()).isTrue();
        assertThat(result.get(1).id()).isEqualTo("pm_other");
        assertThat(result.get(1).isDefault()).isFalse();
    }

    @Test
    void returns_empty_list_and_never_calls_stripe_when_user_has_no_customer() {
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.empty());

        List<SavedPaymentMethodReadModel> result = handler.handle(new ListPaymentMethodsQuery(userId));

        assertThat(result).isEmpty();
        verify(paymentGateway, never()).listPaymentMethods(any());
    }

    @Test
    void fails_soft_to_empty_list_when_stripe_errors() {
        when(stripeCustomerRepository.findStripeCustomerIdByUserId(userId))
                .thenReturn(Optional.of("cus_123"));
        when(paymentGateway.listPaymentMethods("cus_123"))
                .thenThrow(new PaymentGatewayException("stripe down", new RuntimeException()));

        List<SavedPaymentMethodReadModel> result = handler.handle(new ListPaymentMethodsQuery(userId));

        assertThat(result).isEmpty();
    }
}
