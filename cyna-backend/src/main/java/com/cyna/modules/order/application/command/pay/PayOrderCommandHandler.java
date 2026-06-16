package com.cyna.modules.order.application.command.pay;

import com.cyna.modules.order.domain.repository.OrderRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class PayOrderCommandHandler implements CommandHandler<PayOrderCommand, Void> {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public PayOrderCommandHandler(OrderRepository orderRepository,
                                  DomainEventPublisher eventPublisher,
                                  TransactionRunner transactionRunner) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(PayOrderCommand command) {
        return transactionRunner.runReturning(() -> {
            var order = orderRepository.findById(command.orderId()).orElse(null);
            if (order == null) {
                return Result.failure("ORDER_NOT_FOUND");
            }

            return order.pay().map(paid -> {
                orderRepository.save(paid);
                eventPublisher.publishAll(paid.getDomainEvents());
                paid.clearDomainEvents();
                return (Void) null;
            });
        });
    }
}
