package com.cyna.modules.order.domain.model;

import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Money;

public record OrderTotals(
        Money subtotal,
        Money vatAmount,
        Money totalTtc
) {
    public OrderTotals {
        Guard.againstNull(subtotal, "subtotal");
        Guard.againstNull(vatAmount, "vatAmount");
        Guard.againstNull(totalTtc, "totalTtc");
    }
}
