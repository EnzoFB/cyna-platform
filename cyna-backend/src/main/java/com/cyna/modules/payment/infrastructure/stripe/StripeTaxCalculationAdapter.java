package com.cyna.modules.payment.infrastructure.stripe;

import com.cyna.modules.payment.config.StripeProperties;
import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.TaxCalculationPort;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.tax.Calculation;
import com.stripe.param.tax.CalculationCreateParams;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stripe implementation of {@link TaxCalculationPort} — the read-only "exact VAT"
 * preview via the Stripe Tax Calculations API. Kept as its own adapter (separate
 * from {@code StripePaymentAdapter}) so the focused port has a focused bean: a
 * test can mock {@code PaymentGatewayPort} without taking the tax preview down
 * with it.
 */
@Component
public class StripeTaxCalculationAdapter implements TaxCalculationPort {

    private final StripeProperties properties;

    public StripeTaxCalculationAdapter(StripeProperties properties) {
        this.properties = properties;
        Stripe.apiKey = properties.secretKey();
    }

    @Override
    public TaxCalculationResult calculateTax(TaxCalculationRequest request) {
        // Stripe Tax off → no calculation possible; let the caller fall back to
        // its own estimate rather than erroring.
        if (!properties.taxEnabled()) {
            return TaxCalculationResult.disabled();
        }
        try {
            CalculationCreateParams.Builder params = CalculationCreateParams.builder()
                    .setCurrency(request.currency().toLowerCase());

            for (TaxCalculationRequest.TaxLine line : request.lines()) {
                // Line total (HT) in minor units: unit price × quantity.
                long amountCents = line.unitAmountHt()
                        .multiply(BigDecimal.valueOf(100))
                        .multiply(BigDecimal.valueOf(line.quantity()))
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact();
                params.addLineItem(
                        CalculationCreateParams.LineItem.builder()
                                .setReference(line.reference())
                                .setAmount(amountCents)
                                .setQuantity((long) line.quantity())
                                // Same tax treatment as at subscription creation:
                                // catalog prices are HT, the product tax code is SaaS.
                                .setTaxBehavior(CalculationCreateParams.LineItem.TaxBehavior.EXCLUSIVE)
                                .setTaxCode(properties.taxCode())
                                .build());
            }

            CalculationCreateParams.CustomerDetails.Address.Builder address =
                    CalculationCreateParams.CustomerDetails.Address.builder()
                            .setCountry(request.countryCode());
            if (request.postalCode() != null && !request.postalCode().isBlank()) {
                address.setPostalCode(request.postalCode());
            }
            if (request.state() != null && !request.state().isBlank()) {
                address.setState(request.state());
            }

            CalculationCreateParams.CustomerDetails.Builder customer =
                    CalculationCreateParams.CustomerDetails.builder()
                            .setAddress(address.build())
                            .setAddressSource(
                                    CalculationCreateParams.CustomerDetails.AddressSource.BILLING);

            // B2B VAT number → drives the reverse charge in the preview, exactly
            // like the real charge. Unknown prefix is simply omitted (B2C result).
            if (request.vatNumber() != null && !request.vatNumber().isBlank()) {
                String vat = VatNumbers.normalize(request.vatNumber());
                CalculationCreateParams.CustomerDetails.TaxId.Type type = calcTaxIdTypeFor(vat);
                if (type != null) {
                    customer.addTaxId(
                            CalculationCreateParams.CustomerDetails.TaxId.builder()
                                    .setType(type)
                                    .setValue(vat)
                                    .build());
                }
            }

            params.setCustomerDetails(customer.build());

            Calculation calc = Calculation.create(params.build());

            long totalCents = calc.getAmountTotal();             // TTC
            long taxCents = calc.getTaxAmountExclusive();        // VAT added on top
            long htCents = totalCents - taxCents;                // HT
            // Stripe tags each breakdown with a taxability reason; "reverse_charge"
            // is the authoritative signal that the intra-EU autoliquidation applied.
            boolean reverseCharge = calc.getTaxBreakdown() != null
                    && calc.getTaxBreakdown().stream()
                    .anyMatch(b -> "reverse_charge".equals(b.getTaxabilityReason()));

            return new TaxCalculationResult(
                    true,
                    BigDecimal.valueOf(htCents).movePointLeft(2),
                    BigDecimal.valueOf(taxCents).movePointLeft(2),
                    BigDecimal.valueOf(totalCents).movePointLeft(2),
                    calc.getCurrency() != null ? calc.getCurrency().toUpperCase() : request.currency(),
                    reverseCharge);
        } catch (StripeException e) {
            throw new PaymentGatewayException(
                    "Stripe Tax calculation failed: " + e.getMessage(), e);
        }
    }

    private static CalculationCreateParams.CustomerDetails.TaxId.Type calcTaxIdTypeFor(String vatNumber) {
        return switch (VatNumbers.regionOf(vatNumber)) {
            case EU -> CalculationCreateParams.CustomerDetails.TaxId.Type.EU_VAT;
            case GB -> CalculationCreateParams.CustomerDetails.TaxId.Type.GB_VAT;
            case CH -> CalculationCreateParams.CustomerDetails.TaxId.Type.CH_VAT;
            case NO -> CalculationCreateParams.CustomerDetails.TaxId.Type.NO_VAT;
            case UNKNOWN -> null;
        };
    }
}
