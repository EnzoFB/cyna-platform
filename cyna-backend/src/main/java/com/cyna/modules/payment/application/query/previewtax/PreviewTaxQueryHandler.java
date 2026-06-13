package com.cyna.modules.payment.application.query.previewtax;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.TaxCalculationPort;
import com.cyna.modules.payment.domain.port.TaxCalculationPort.TaxCalculationRequest;
import com.cyna.modules.payment.domain.port.TaxCalculationPort.TaxCalculationResult;
import com.cyna.modules.product.application.api.ProductInfo;
import com.cyna.modules.product.application.api.ProductQueryApi;
import com.cyna.shared.application.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PreviewTaxQueryHandler
        implements QueryHandler<PreviewTaxQuery, TaxPreviewReadModel> {

    private static final Logger log = LoggerFactory.getLogger(PreviewTaxQueryHandler.class);

    private final ProductQueryApi productQueryApi;
    private final TaxCalculationPort taxCalculationPort;

    public PreviewTaxQueryHandler(ProductQueryApi productQueryApi,
                                  TaxCalculationPort taxCalculationPort) {
        this.productQueryApi = productQueryApi;
        this.taxCalculationPort = taxCalculationPort;
    }

    @Override
    public TaxPreviewReadModel handle(PreviewTaxQuery query) {
        String currency = query.currency() != null && !query.currency().isBlank()
                ? query.currency() : "EUR";

        if (query.lines() == null || query.lines().isEmpty() || query.countryCode() == null) {
            return TaxPreviewReadModel.estimateFallback(currency);
        }

        // Resolve unit prices server-side — the client never dictates the taxed
        // amount. Unknown products are skipped (stale cart line).
        List<UUID> productIds = query.lines().stream()
                .map(PreviewTaxQuery.Line::productId)
                .toList();
        Map<UUID, ProductInfo> products = productQueryApi.getByIds(productIds).stream()
                .collect(Collectors.toMap(ProductInfo::id, Function.identity(), (a, b) -> a));

        List<TaxCalculationRequest.TaxLine> taxLines = new ArrayList<>();
        for (PreviewTaxQuery.Line line : query.lines()) {
            ProductInfo product = products.get(line.productId());
            if (product == null) {
                continue;
            }
            BigDecimal unitHt = "ANNUAL".equals(line.billingCycle())
                    ? product.annualPrice()
                    : product.monthlyPrice();
            int qty = Math.max(line.quantity(), 1);
            taxLines.add(new TaxCalculationRequest.TaxLine(
                    line.productId().toString(), unitHt, qty));
        }
        if (taxLines.isEmpty()) {
            return TaxPreviewReadModel.estimateFallback(currency);
        }

        try {
            TaxCalculationResult result = taxCalculationPort.calculateTax(new TaxCalculationRequest(
                    currency, taxLines, query.countryCode(),
                    query.postalCode(), query.state(), query.vatNumber()));

            if (!result.enabled()) {
                return TaxPreviewReadModel.estimateFallback(currency);
            }
            return new TaxPreviewReadModel(
                    true,
                    result.subtotalHt(),
                    result.vatAmount(),
                    result.totalTtc(),
                    result.currency(),
                    result.reverseCharge());
        } catch (PaymentGatewayException e) {
            // Preview is a UX nicety — never block the checkout on it. Fall back
            // to the client estimate; the authoritative VAT is still computed by
            // Stripe at subscription creation.
            log.warn("[tax-preview] Stripe Tax calculation failed: {}", e.getMessage());
            return TaxPreviewReadModel.estimateFallback(currency);
        }
    }
}
