package com.cyna.modules.cart.domain.model;

import com.cyna.shared.domain.Guard;

import java.math.BigDecimal;

/**
 * Cart-side totals: HT subtotal only. VAT is not computed at cart preview time —
 * it is determined at checkout by the payment module via Stripe Tax (country of
 * the billing address, B2B reverse charge, etc.). Cart used to embed a hardcoded
 * 20% rate which lied as soon as the customer was outside FR or B2B.
 */
public record CartTotals(
        BigDecimal subtotalHt,
        String currency
) {
    public CartTotals {
        Guard.againstNull(subtotalHt, "subtotalHt");
        Guard.againstNullOrBlank(currency, "currency");
    }
}
