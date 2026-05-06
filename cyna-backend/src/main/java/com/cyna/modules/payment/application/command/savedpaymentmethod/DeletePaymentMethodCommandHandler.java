package com.cyna.modules.payment.application.command.savedpaymentmethod;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.SavedPaymentMethodRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletePaymentMethodCommandHandler
        implements CommandHandler<DeletePaymentMethodCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(DeletePaymentMethodCommandHandler.class);

    private final SavedPaymentMethodRepository repository;
    private final PaymentGatewayPort paymentGateway;

    public DeletePaymentMethodCommandHandler(SavedPaymentMethodRepository repository,
                                             PaymentGatewayPort paymentGateway) {
        this.repository = repository;
        this.paymentGateway = paymentGateway;
    }

    @Override
    @Transactional
    public Result<Void> handle(DeletePaymentMethodCommand command) {
        var method = repository.findByIdAndUserId(command.paymentMethodId(), command.userId())
                .orElse(null);

        if (method == null) {
            return Result.failure("NOT_FOUND");
        }

        try {
            paymentGateway.detachPaymentMethod(method.getStripePaymentMethodId());
        } catch (PaymentGatewayException e) {
            log.error("[delete-pm] Stripe detach failed for pm {} user {}: {}",
                    method.getStripePaymentMethodId(), command.userId(), e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }

        repository.deleteById(method.getId());

        // If we deleted the default, promote the oldest remaining card
        if (method.isDefault()) {
            var remaining = repository.findAllByUserId(command.userId());
            if (!remaining.isEmpty()) {
                var promoted = remaining.get(remaining.size() - 1).withDefault(true);
                repository.save(promoted);
            }
        }

        return Result.success();
    }
}
