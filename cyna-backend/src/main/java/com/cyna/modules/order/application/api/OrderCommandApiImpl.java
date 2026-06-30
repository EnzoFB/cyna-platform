package com.cyna.modules.order.application.api;

import com.cyna.modules.order.application.command.create.CreateOrderCommand;
import com.cyna.modules.order.application.command.pay.PayOrderCommand;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class OrderCommandApiImpl implements OrderCommandApi {

    private final Mediator mediator;

    OrderCommandApiImpl(Mediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public Result<Void> markOrderAsPaid(UUID orderId, String lang) {
        return mediator.send(new PayOrderCommand(orderId, lang));
    }

    @Override
    public Result<UUID> createOrderFromCart(UUID userId,
                                            List<CartLineRequest> lines,
                                            BillingAddressRequest billingAddress) {
        List<CreateOrderCommand.CreateOrderLine> commandLines = lines.stream()
                .map(l -> new CreateOrderCommand.CreateOrderLine(l.productId(), l.billingCycle(), l.quantity()))
                .toList();
        CreateOrderCommand.BillingAddress commandBilling = billingAddress == null
                ? null
                : new CreateOrderCommand.BillingAddress(
                        billingAddress.line1(),
                        billingAddress.city(),
                        billingAddress.zipCode(),
                        billingAddress.countryCode());
        return mediator.send(new CreateOrderCommand(userId, commandLines, commandBilling));
    }
}
