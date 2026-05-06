package com.cyna.modules.payment.application.command.savedpaymentmethod;

import com.cyna.modules.payment.domain.model.SavedPaymentMethod;
import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.SavedPaymentMethodRepository;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SavePaymentMethodCommandHandler
        implements CommandHandler<SavePaymentMethodCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(SavePaymentMethodCommandHandler.class);

    private final SavedPaymentMethodRepository repository;
    private final StripeCustomerRepository stripeCustomerRepository;
    private final PaymentGatewayPort paymentGateway;

    public SavePaymentMethodCommandHandler(SavedPaymentMethodRepository repository,
                                           StripeCustomerRepository stripeCustomerRepository,
                                           PaymentGatewayPort paymentGateway) {
        this.repository = repository;
        this.stripeCustomerRepository = stripeCustomerRepository;
        this.paymentGateway = paymentGateway;
    }

    @Override
    @Transactional
    public Result<Void> handle(SavePaymentMethodCommand command) {
        var customerId = stripeCustomerRepository
                .findStripeCustomerIdByUserId(command.userId())
                .orElse(null);

        if (customerId == null) {
            return Result.failure("NO_STRIPE_CUSTOMER");
        }

        try {
            var details = paymentGateway.attachPaymentMethod(customerId, command.stripePaymentMethodId());

            boolean isFirst = repository.countByUserId(command.userId()) == 0;

            var method = SavedPaymentMethod.create(
                    command.userId(),
                    command.stripePaymentMethodId(),
                    details.brand(),
                    details.last4(),
                    details.expMonth(),
                    details.expYear(),
                    details.holderName(),
                    isFirst
            );

            repository.save(method);
            return Result.success();

        } catch (PaymentGatewayException e) {
            log.error("[save-pm] Stripe error for user {}: {}", command.userId(), e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }
    }
}
