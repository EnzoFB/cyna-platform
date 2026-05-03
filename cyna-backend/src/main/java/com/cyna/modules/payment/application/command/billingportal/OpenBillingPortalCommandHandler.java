package com.cyna.modules.payment.application.command.billingportal;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenBillingPortalCommandHandler
        implements CommandHandler<OpenBillingPortalCommand, String> {

    private static final Logger log = LoggerFactory.getLogger(OpenBillingPortalCommandHandler.class);

    private final StripeCustomerRepository stripeCustomerRepository;
    private final PaymentGatewayPort paymentGateway;

    public OpenBillingPortalCommandHandler(StripeCustomerRepository stripeCustomerRepository,
                                           PaymentGatewayPort paymentGateway) {
        this.stripeCustomerRepository = stripeCustomerRepository;
        this.paymentGateway = paymentGateway;
    }

    @Override
    public Result<String> handle(OpenBillingPortalCommand command) {
        var stripeCustomerId = stripeCustomerRepository
                .findStripeCustomerIdByUserId(command.userId())
                .orElse(null);

        if (stripeCustomerId == null) {
            // The user has never paid, so no Stripe Customer exists for them yet.
            return Result.failure("NO_STRIPE_CUSTOMER");
        }

        try {
            String url = paymentGateway.createBillingPortalSession(stripeCustomerId, command.returnUrl());
            return Result.success(url);
        } catch (PaymentGatewayException e) {
            log.error("Billing portal session creation failed for user {}: {}",
                    command.userId(), e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }
    }
}
