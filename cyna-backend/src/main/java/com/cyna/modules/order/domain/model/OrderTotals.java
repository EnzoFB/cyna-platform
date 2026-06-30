package com.cyna.modules.order.domain.model;

import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Money;

/**
 * Cyna stores the HT subtotal only. VAT and TTC live in Stripe (the
 * authoritative source for what was actually billed, with the correct
 * destination tax and B2B reverse charge handling). See migration V3.
 */
public record OrderTotals(
        Money subtotalHt
) {
    public OrderTotals {
        Guard.againstNull(subtotalHt, "subtotalHt");
    }
}
