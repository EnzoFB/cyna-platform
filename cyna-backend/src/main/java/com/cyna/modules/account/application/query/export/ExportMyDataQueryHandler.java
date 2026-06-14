package com.cyna.modules.account.application.query.export;

import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.application.api.PaymentQueryApi;
import com.cyna.modules.subscription.application.api.SubscriptionQueryApi;
import com.cyna.modules.user.application.api.UserPersonalDataView;
import com.cyna.modules.user.application.api.UserQueryApi;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Assembles the RGPD Art. 15/20 personal-data export by orchestrating the
 * published APIs of the user, order, subscription and payment modules. Holds no
 * persistence of its own — the {@code account} module is a pure orchestrator,
 * which keeps the module graph acyclic.
 */
@Component
public class ExportMyDataQueryHandler
        implements QueryHandler<ExportMyDataQuery, MyDataExport> {

    private static final String NOTICE =
            "Invoices are not duplicated here: they are legally retained by our "
            + "payment processor Stripe (French Code de commerce L123-22, 10 years) "
            + "and downloadable from your billing history. Payment card numbers are "
            + "never stored by us (PCI-DSS SAQ A — tokenized at Stripe).";

    private final UserQueryApi userQueryApi;
    private final OrderQueryApi orderQueryApi;
    private final SubscriptionQueryApi subscriptionQueryApi;
    private final PaymentQueryApi paymentQueryApi;

    public ExportMyDataQueryHandler(UserQueryApi userQueryApi,
                                    OrderQueryApi orderQueryApi,
                                    SubscriptionQueryApi subscriptionQueryApi,
                                    PaymentQueryApi paymentQueryApi) {
        this.userQueryApi = userQueryApi;
        this.orderQueryApi = orderQueryApi;
        this.subscriptionQueryApi = subscriptionQueryApi;
        this.paymentQueryApi = paymentQueryApi;
    }

    @Override
    public MyDataExport handle(ExportMyDataQuery query) {
        UserPersonalDataView userData = userQueryApi.exportPersonalData(query.userId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        var a = userData.account();
        var account = new MyDataExport.Account(
                a.id(), a.email(), a.firstName(), a.lastName(),
                a.company(), a.role(), a.status(), a.createdAt());

        List<MyDataExport.AddressEntry> addresses = userData.addresses().stream()
                .map(addr -> new MyDataExport.AddressEntry(
                        addr.id(), addr.firstName(), addr.lastName(), addr.label(),
                        addr.address(), addr.address2(), addr.zipCode(), addr.city(),
                        addr.region(), addr.countryCode(), addr.phone(), addr.company(),
                        addr.vatNumber(), addr.isDefault()))
                .toList();

        List<MyDataExport.ConsentEntry> termsConsents = userData.termsConsents().stream()
                .map(c -> new MyDataExport.ConsentEntry(
                        c.action(), c.labelVersion(), c.ipAddress(), c.userAgent(), c.givenAt()))
                .toList();

        return new MyDataExport(
                Instant.now(),
                account,
                addresses,
                orderQueryApi.exportOrdersForUser(query.userId()),
                subscriptionQueryApi.exportForUser(query.userId()),
                paymentQueryApi.exportConsentsForUser(query.userId()),
                termsConsents,
                NOTICE
        );
    }
}
