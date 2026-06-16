package com.cyna.modules.payment.application.query.listpaymentmethods;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.shared.application.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListPaymentMethodsQueryHandler
        implements QueryHandler<ListPaymentMethodsQuery, List<SavedPaymentMethodReadModel>> {

    private static final Logger log = LoggerFactory.getLogger(ListPaymentMethodsQueryHandler.class);

    private final StripeCustomerRepository stripeCustomerRepository;
    private final PaymentGatewayPort paymentGateway;

    public ListPaymentMethodsQueryHandler(StripeCustomerRepository stripeCustomerRepository,
                                          PaymentGatewayPort paymentGateway) {
        this.stripeCustomerRepository = stripeCustomerRepository;
        this.paymentGateway = paymentGateway;
    }

    @Override
    public List<SavedPaymentMethodReadModel> handle(ListPaymentMethodsQuery query) {
        // Stripe is the single source of truth for cards: read them live so the
        // "My payment methods" page can never diverge from the Stripe Portal.
        // No Stripe customer yet → the user never saved a card → empty list.
        var stripeCustomerId = stripeCustomerRepository
                .findStripeCustomerIdByUserId(query.userId())
                .orElse(null);
        if (stripeCustomerId == null) {
            return List.of();
        }

        try {
            return paymentGateway.listPaymentMethods(stripeCustomerId).stream()
                    .map(SavedPaymentMethodReadModel::fromStripe)
                    .toList();
        } catch (PaymentGatewayException e) {
            // Stripe glitch — fail soft so the rest of the account page still
            // renders. Nothing is lost (Stripe holds the cards); the user retries.
            log.error("[payment-methods] Stripe list failed for user {}: {}",
                    query.userId(), e.getMessage());
            return List.of();
        }
    }
}
