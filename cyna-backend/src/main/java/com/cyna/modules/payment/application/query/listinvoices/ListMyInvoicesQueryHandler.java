package com.cyna.modules.payment.application.query.listinvoices;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.shared.application.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ListMyInvoicesQueryHandler
        implements QueryHandler<ListMyInvoicesQuery, List<InvoiceReadModel>> {

    private static final Logger log = LoggerFactory.getLogger(ListMyInvoicesQueryHandler.class);

    private final StripeCustomerRepository stripeCustomerRepository;
    private final PaymentGatewayPort paymentGateway;

    public ListMyInvoicesQueryHandler(StripeCustomerRepository stripeCustomerRepository,
                                      PaymentGatewayPort paymentGateway) {
        this.stripeCustomerRepository = stripeCustomerRepository;
        this.paymentGateway = paymentGateway;
    }

    @Override
    public List<InvoiceReadModel> handle(ListMyInvoicesQuery query) {
        // No Stripe customer yet → the user never paid → no invoices. Return
        // empty rather than erroring: the billing history page just shows
        // "no invoices".
        var stripeCustomerId = stripeCustomerRepository
                .findStripeCustomerIdByUserId(query.userId())
                .orElse(null);
        if (stripeCustomerId == null) {
            return List.of();
        }

        try {
            return paymentGateway.listInvoices(stripeCustomerId).stream()
                    .map(i -> new InvoiceReadModel(
                            i.id(), i.number(), i.status(), i.amountPaid(),
                            i.currency(), i.createdAt(),
                            i.hostedInvoiceUrl(), i.invoicePdfUrl()))
                    .sorted(Comparator.comparing(InvoiceReadModel::createdAt).reversed())
                    .toList();
        } catch (PaymentGatewayException e) {
            // Stripe glitch — fail soft so the rest of the account page still
            // renders. The user can retry; nothing is lost (Stripe is the SoR).
            log.error("[invoices] Stripe list failed for user {}: {}", query.userId(), e.getMessage());
            return List.of();
        }
    }
}
