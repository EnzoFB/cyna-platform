package com.cyna.modules.cart.application.model;

import java.util.List;
import java.util.UUID;

public record CartReadModel(
        UUID cartId,
        UUID userId,
        String status,
        List<CartLineReadModel> lines,
        CartTotalsReadModel totals,
        boolean checkoutAllowed,
        List<String> errors
) {
}
