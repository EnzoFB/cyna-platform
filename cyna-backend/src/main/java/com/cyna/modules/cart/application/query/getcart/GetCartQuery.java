package com.cyna.modules.cart.application.query.getcart;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.shared.application.Query;

import java.util.UUID;

public record GetCartQuery(
        UUID userId,
        String guestToken
) implements Query<CartReadModel> {
}
