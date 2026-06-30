package com.cyna.modules.payment.application.api;

import com.cyna.modules.payment.domain.model.OrderTaxSnapshot;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.OrderTaxSnapshotRepository;
import com.cyna.modules.payment.domain.repository.PaymentConsentLogRepository;
import com.cyna.modules.subscription.application.api.SubscriptionQueryApi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class PaymentQueryApiImpl implements PaymentQueryApi {

    private final PaymentConsentLogRepository consentLogRepository;
    private final SubscriptionQueryApi subscriptionQueryApi;
    private final PaymentGatewayPort paymentGatewayPort;
    private final OrderTaxSnapshotRepository orderTaxSnapshotRepository;

    PaymentQueryApiImpl(PaymentConsentLogRepository consentLogRepository,
                        SubscriptionQueryApi subscriptionQueryApi,
                        PaymentGatewayPort paymentGatewayPort,
                        OrderTaxSnapshotRepository orderTaxSnapshotRepository) {
        this.consentLogRepository = consentLogRepository;
        this.subscriptionQueryApi = subscriptionQueryApi;
        this.paymentGatewayPort = paymentGatewayPort;
        this.orderTaxSnapshotRepository = orderTaxSnapshotRepository;
    }

    @Override
    public OrderTaxSummaryView getOrderTaxSummary(UUID userId, UUID orderId) {
        // 1. Authoritative snapshot captured at payment time — a local read, no
        // Stripe call. Present for every order paid through the normal finalize
        // flow, written in the same transaction that flips the order to PAID.
        Optional<OrderTaxSnapshot> snapshot =
                orderTaxSnapshotRepository.findByOrderIdAndUserId(orderId, userId);
        if (snapshot.isPresent()) {
            OrderTaxSnapshot s = snapshot.get();
            return new OrderTaxSummaryView(
                    true, s.subtotalHt(), s.vatAmount(), s.totalTtc(), s.currency(), s.reverseCharge());
        }

        // 2. Fallback: orders paid before this feature existed, or provisioned
        // only via the webhook-reconcile path (finalize couldn't persist). Read
        // the VAT/TTC live from the Stripe invoices.
        List<String> stripeSubscriptionIds =
                subscriptionQueryApi.findStripeSubscriptionIdsForOrder(userId, orderId);
        if (stripeSubscriptionIds.isEmpty()) {
            return OrderTaxSummaryView.unavailable();
        }
        return paymentGatewayPort.getOrderTaxFromInvoices(stripeSubscriptionIds)
                .map(s -> new OrderTaxSummaryView(
                        true, s.subtotalHt(), s.vatAmount(), s.totalTtc(), s.currency(), s.reverseCharge()))
                .orElseGet(OrderTaxSummaryView::unavailable);
    }

    @Override
    public List<PaymentConsentExportView> exportConsentsForUser(UUID userId) {
        return consentLogRepository.findAllByUserId(userId).stream()
                .map(c -> new PaymentConsentExportView(
                        c.getAction().name(),
                        c.getLabelVersion(),
                        c.getStripePaymentMethodId(),
                        c.getIpAddress(),
                        c.getUserAgent(),
                        c.getGivenAt()))
                .toList();
    }
}
