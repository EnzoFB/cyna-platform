package com.cyna.modules.user.application.query.export;

import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.application.api.PaymentQueryApi;
import com.cyna.modules.subscription.application.api.SubscriptionQueryApi;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.AddressRepository;
import com.cyna.modules.user.domain.repository.UserConsentLogRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ExportMyDataQueryHandler
        implements QueryHandler<ExportMyDataQuery, MyDataExport> {

    private static final String NOTICE =
            "Invoices are not duplicated here: they are legally retained by our "
            + "payment processor Stripe (French Code de commerce L123-22, 10 years) "
            + "and downloadable from your billing history. Payment card numbers are "
            + "never stored by us (PCI-DSS SAQ A — tokenized at Stripe).";

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserConsentLogRepository userConsentLogRepository;
    private final OrderQueryApi orderQueryApi;
    private final SubscriptionQueryApi subscriptionQueryApi;
    private final PaymentQueryApi paymentQueryApi;

    public ExportMyDataQueryHandler(UserRepository userRepository,
                                    AddressRepository addressRepository,
                                    UserConsentLogRepository userConsentLogRepository,
                                    OrderQueryApi orderQueryApi,
                                    SubscriptionQueryApi subscriptionQueryApi,
                                    PaymentQueryApi paymentQueryApi) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.userConsentLogRepository = userConsentLogRepository;
        this.orderQueryApi = orderQueryApi;
        this.subscriptionQueryApi = subscriptionQueryApi;
        this.paymentQueryApi = paymentQueryApi;
    }

    @Override
    public MyDataExport handle(ExportMyDataQuery query) {
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        var account = new MyDataExport.Account(
                user.getId(), user.getEmail().value(), user.getFirstName(),
                user.getLastName(), user.getCompany(), user.getRole().name(),
                user.getStatus().name(), user.getCreatedAt());

        List<MyDataExport.AddressEntry> addresses = addressRepository.findAllByUserId(query.userId())
                .stream()
                .map(a -> new MyDataExport.AddressEntry(
                        a.getId(), a.getFirstName(), a.getLastName(), a.getLabel(),
                        a.getAddress(), a.getAddress2(), a.getZipCode(), a.getCity(),
                        a.getRegion(), a.getCountryCode(), a.getPhone(), a.getCompany(),
                        a.getVatNumber(), a.isDefault()))
                .toList();

        List<MyDataExport.ConsentEntry> termsConsents =
                userConsentLogRepository.findAllByUserId(query.userId()).stream()
                        .map(c -> new MyDataExport.ConsentEntry(
                                c.getAction().name(), c.getLabelVersion(),
                                c.getIpAddress(), c.getUserAgent(), c.getGivenAt()))
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
