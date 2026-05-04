package com.cyna.modules.payment.application.command.processresult;

import com.cyna.modules.payment.domain.model.PaymentStatus;
import com.cyna.modules.payment.domain.repository.PaymentRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class ProcessPaymentResultCommandHandler
        implements CommandHandler<ProcessPaymentResultCommand, Void> {

    private final PaymentRepository paymentRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public ProcessPaymentResultCommandHandler(PaymentRepository paymentRepository,
                                              DomainEventPublisher eventPublisher,
                                              TransactionRunner transactionRunner) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(ProcessPaymentResultCommand command) {
        return transactionRunner.runReturning(() -> {
            var payment = paymentRepository
                    .findByStripePaymentIntentId(command.stripePaymentIntentId())
                    .orElse(null);

            if (payment == null) {
                // PaymentIntent inconnu : on renvoie 200 à Stripe pour éviter les retentatives infinies
                return Result.success();
            }

            // Idempotence
            if (command.succeeded() && payment.getStatus() == PaymentStatus.SUCCEEDED) {
                return Result.success();
            }
            if (!command.succeeded() && payment.getStatus() == PaymentStatus.FAILED) {
                return Result.success();
            }

            var updatedResult = command.succeeded() ? payment.markSucceeded() : payment.markFailed();

            return updatedResult.map(updated -> {
                paymentRepository.save(updated);
                eventPublisher.publishAll(updated.getDomainEvents());
                updated.clearDomainEvents();
                return (Void) null;
            });
        });
    }
}
