package com.cyna.modules.payment.application.command.savedpaymentmethod;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.modules.user.application.api.UserQueryApi;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CreateSetupIntentCommandHandler
        implements CommandHandler<CreateSetupIntentCommand, String> {

    private static final Logger log = LoggerFactory.getLogger(CreateSetupIntentCommandHandler.class);

    private final StripeCustomerRepository stripeCustomerRepository;
    private final PaymentGatewayPort paymentGateway;
    private final UserQueryApi userQueryApi;

    public CreateSetupIntentCommandHandler(StripeCustomerRepository stripeCustomerRepository,
                                           PaymentGatewayPort paymentGateway,
                                           UserQueryApi userQueryApi) {
        this.stripeCustomerRepository = stripeCustomerRepository;
        this.paymentGateway = paymentGateway;
        this.userQueryApi = userQueryApi;
    }

    @Override
    public Result<String> handle(CreateSetupIntentCommand command) {
        try {
            String customerId = stripeCustomerRepository
                    .findStripeCustomerIdByUserId(command.userId())
                    .orElseGet(() -> {
                        var user = userQueryApi.findUserForPayment(command.userId())
                                .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));
                        String newCustomerId = paymentGateway.createCustomerForUser(
                                user.email(), user.firstName() + " " + user.lastName());
                        stripeCustomerRepository.save(command.userId(), newCustomerId);
                        return newCustomerId;
                    });

            String clientSecret = paymentGateway.createSetupIntent(customerId);
            return Result.success(clientSecret);

        } catch (IllegalStateException e) {
            return Result.failure(e.getMessage());
        } catch (PaymentGatewayException e) {
            log.error("[setup-intent] Stripe error for user {}: {}", command.userId(), e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }
    }
}
