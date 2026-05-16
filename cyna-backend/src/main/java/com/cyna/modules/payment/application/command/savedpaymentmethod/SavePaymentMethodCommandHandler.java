package com.cyna.modules.payment.application.command.savedpaymentmethod;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Persists a card the user collected at checkout by <em>attaching it to their
 * Stripe Customer</em>. Stripe is the single source of truth: once attached,
 * the card shows up in the live "My payment methods" listing and the Customer
 * Portal. We keep no local mirror — there is nothing else to store here.
 */
@Component
public class SavePaymentMethodCommandHandler
        implements CommandHandler<SavePaymentMethodCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(SavePaymentMethodCommandHandler.class);

    private final StripeCustomerRepository stripeCustomerRepository;
    private final PaymentGatewayPort paymentGateway;

    public SavePaymentMethodCommandHandler(StripeCustomerRepository stripeCustomerRepository,
                                           PaymentGatewayPort paymentGateway) {
        this.stripeCustomerRepository = stripeCustomerRepository;
        this.paymentGateway = paymentGateway;
    }

    @Override
    public Result<Void> handle(SavePaymentMethodCommand command) {
        var customerId = stripeCustomerRepository
                .findStripeCustomerIdByUserId(command.userId())
                .orElse(null);

        if (customerId == null) {
            return Result.failure("NO_STRIPE_CUSTOMER");
        }

        try {
            // Attach to the Stripe Customer; the returned display metadata is
            // intentionally unused — the listing reads it live from Stripe.
            paymentGateway.attachPaymentMethod(customerId, command.stripePaymentMethodId());
            return Result.success();
        } catch (PaymentGatewayException e) {
            log.error("[save-pm] Stripe error for user {}: {}", command.userId(), e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }
    }
}
