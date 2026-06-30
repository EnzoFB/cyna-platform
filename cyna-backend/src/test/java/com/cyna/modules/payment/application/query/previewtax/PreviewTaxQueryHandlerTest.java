package com.cyna.modules.payment.application.query.previewtax;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.TaxCalculationPort;
import com.cyna.modules.payment.domain.port.TaxCalculationPort.TaxCalculationRequest;
import com.cyna.modules.payment.domain.port.TaxCalculationPort.TaxCalculationResult;
import com.cyna.modules.product.application.api.ProductInfo;
import com.cyna.modules.product.application.api.ProductQueryApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreviewTaxQueryHandlerTest {

    @Mock private ProductQueryApi productQueryApi;
    @Mock private TaxCalculationPort taxCalculationPort;

    private PreviewTaxQueryHandler handler() {
        return new PreviewTaxQueryHandler(productQueryApi, taxCalculationPort);
    }

    @Test
    void should_resolve_prices_server_side_and_map_the_exact_stripe_result() {
        UUID productId = UUID.randomUUID();
        when(productQueryApi.getByIds(any())).thenReturn(List.of(
                product(productId, BigDecimal.valueOf(100), BigDecimal.valueOf(1000))));
        when(taxCalculationPort.calculateTax(any())).thenReturn(new TaxCalculationResult(
                true, BigDecimal.valueOf(200), BigDecimal.valueOf(40),
                BigDecimal.valueOf(240), "EUR", false));

        // 2× MONTHLY @100 HT.
        var query = new PreviewTaxQuery("EUR",
                List.of(new PreviewTaxQuery.Line(productId, "MONTHLY", 2)),
                "FR", "75001", null, null);

        TaxPreviewReadModel result = handler().handle(query);

        // The line sent to Stripe uses the SERVER price (100), not anything from
        // the client, with the unit amount and quantity preserved.
        ArgumentCaptor<TaxCalculationRequest> captor = ArgumentCaptor.forClass(TaxCalculationRequest.class);
        verify(taxCalculationPort).calculateTax(captor.capture());
        TaxCalculationRequest sent = captor.getValue();
        assertThat(sent.lines()).hasSize(1);
        assertThat(sent.lines().get(0).unitAmountHt()).isEqualByComparingTo("100");
        assertThat(sent.lines().get(0).quantity()).isEqualTo(2);
        assertThat(sent.countryCode()).isEqualTo("FR");

        assertThat(result.exact()).isTrue();
        assertThat(result.vatAmount()).isEqualByComparingTo("40");
        assertThat(result.totalTtc()).isEqualByComparingTo("240");
        assertThat(result.reverseCharge()).isFalse();
    }

    @Test
    void should_resolve_annual_price_for_annual_lines() {
        UUID productId = UUID.randomUUID();
        when(productQueryApi.getByIds(any())).thenReturn(List.of(
                product(productId, BigDecimal.valueOf(100), BigDecimal.valueOf(1000))));
        when(taxCalculationPort.calculateTax(any())).thenReturn(new TaxCalculationResult(
                true, BigDecimal.valueOf(1000), BigDecimal.ZERO,
                BigDecimal.valueOf(1000), "EUR", true));

        var query = new PreviewTaxQuery("EUR",
                List.of(new PreviewTaxQuery.Line(productId, "ANNUAL", 1)),
                "DE", null, null, "DE123456789");

        TaxPreviewReadModel result = handler().handle(query);

        ArgumentCaptor<TaxCalculationRequest> captor = ArgumentCaptor.forClass(TaxCalculationRequest.class);
        verify(taxCalculationPort).calculateTax(captor.capture());
        assertThat(captor.getValue().lines().get(0).unitAmountHt()).isEqualByComparingTo("1000");
        // Reverse charge flows through to the read model.
        assertThat(result.exact()).isTrue();
        assertThat(result.reverseCharge()).isTrue();
    }

    @Test
    void should_fall_back_to_estimate_when_stripe_tax_is_disabled() {
        UUID productId = UUID.randomUUID();
        when(productQueryApi.getByIds(any())).thenReturn(List.of(
                product(productId, BigDecimal.valueOf(100), BigDecimal.valueOf(1000))));
        when(taxCalculationPort.calculateTax(any())).thenReturn(TaxCalculationResult.disabled());

        var query = new PreviewTaxQuery("EUR",
                List.of(new PreviewTaxQuery.Line(productId, "MONTHLY", 1)),
                "FR", null, null, null);

        TaxPreviewReadModel result = handler().handle(query);

        assertThat(result.exact()).isFalse();
        assertThat(result.subtotalHt()).isNull();
    }

    @Test
    void should_fall_back_to_estimate_when_stripe_errors() {
        UUID productId = UUID.randomUUID();
        when(productQueryApi.getByIds(any())).thenReturn(List.of(
                product(productId, BigDecimal.valueOf(100), BigDecimal.valueOf(1000))));
        when(taxCalculationPort.calculateTax(any()))
                .thenThrow(new PaymentGatewayException("stripe down", null));

        var query = new PreviewTaxQuery("EUR",
                List.of(new PreviewTaxQuery.Line(productId, "MONTHLY", 1)),
                "FR", null, null, null);

        TaxPreviewReadModel result = handler().handle(query);

        // Preview never blocks checkout — it degrades to the estimate.
        assertThat(result.exact()).isFalse();
    }

    @Test
    void should_not_call_stripe_when_there_are_no_lines() {
        var query = new PreviewTaxQuery("EUR", List.of(), "FR", null, null, null);

        TaxPreviewReadModel result = handler().handle(query);

        assertThat(result.exact()).isFalse();
        verify(taxCalculationPort, never()).calculateTax(any());
    }

    @Test
    void should_skip_unknown_products_and_estimate_when_none_resolve() {
        UUID productId = UUID.randomUUID();
        // Product no longer exists (stale cart) → nothing resolves.
        when(productQueryApi.getByIds(any())).thenReturn(List.of());

        var query = new PreviewTaxQuery("EUR",
                List.of(new PreviewTaxQuery.Line(productId, "MONTHLY", 1)),
                "FR", null, null, null);

        TaxPreviewReadModel result = handler().handle(query);

        assertThat(result.exact()).isFalse();
        verify(taxCalculationPort, never()).calculateTax(any());
    }

    private ProductInfo product(UUID id, BigDecimal monthly, BigDecimal annual) {
        return new ProductInfo(
                id, "Test product", "svc", "tech", monthly, annual, "EUR",
                true, true, UUID.randomUUID(), "SOC", 1, 0);
    }
}
