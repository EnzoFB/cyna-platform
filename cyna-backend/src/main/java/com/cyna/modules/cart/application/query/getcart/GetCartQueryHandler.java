package com.cyna.modules.cart.application.query.getcart;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.modules.cart.application.service.CartAccessService;
import com.cyna.modules.cart.application.service.CartReadModelService;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class GetCartQueryHandler implements QueryHandler<GetCartQuery, CartReadModel> {

    private final CartAccessService cartAccessService;
    private final CartReadModelService cartReadModelService;

    public GetCartQueryHandler(CartAccessService cartAccessService, CartReadModelService cartReadModelService) {
        this.cartAccessService = cartAccessService;
        this.cartReadModelService = cartReadModelService;
    }

    @Override
    public CartReadModel handle(GetCartQuery query) {
        return cartAccessService.getOrCreateActiveCart(query.userId(), query.guestToken())
                .map(cartReadModelService::toReadModel)
                .fold(
                        value -> value,
                        error -> {
                            throw new IllegalArgumentException(error);
                        }
                );
    }
}
