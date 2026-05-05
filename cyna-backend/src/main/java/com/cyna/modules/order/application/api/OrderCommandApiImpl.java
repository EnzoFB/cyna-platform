package com.cyna.modules.order.application.api;

import com.cyna.modules.order.application.command.pay.PayOrderCommand;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
class OrderCommandApiImpl implements OrderCommandApi {

    private final Mediator mediator;

    OrderCommandApiImpl(Mediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public Result<Void> markOrderAsPaid(UUID orderId) {
        return mediator.send(new PayOrderCommand(orderId));
    }
}
