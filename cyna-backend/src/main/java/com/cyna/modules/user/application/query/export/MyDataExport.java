package com.cyna.modules.user.application.query.export;

import com.cyna.modules.order.application.api.OrderQueryApi.OrderExportView;
import com.cyna.modules.payment.application.api.PaymentQueryApi.PaymentConsentExportView;
import com.cyna.modules.subscription.application.api.SubscriptionQueryApi.SubscriptionExportView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The full personal-data export (RGPD Art. 15/20). Serialized to JSON and
 * offered as a file download. {@code notice} states the data deliberately not
 * duplicated here (invoices) and where it lives, so the export is honest about
 * its boundaries.
 */
public record MyDataExport(
        Instant exportedAt,
        Account account,
        List<AddressEntry> addresses,
        List<OrderExportView> orders,
        List<SubscriptionExportView> subscriptions,
        List<PaymentConsentExportView> paymentConsents,
        List<ConsentEntry> termsConsents,
        String notice
) {
    public record ConsentEntry(
            String action,
            String labelVersion,
            String ipAddress,
            String userAgent,
            Instant givenAt
    ) {}

    public record Account(
            UUID id,
            String email,
            String firstName,
            String lastName,
            String company,
            String role,
            String status,
            Instant createdAt
    ) {}

    public record AddressEntry(
            UUID id,
            String firstName,
            String lastName,
            String label,
            String address,
            String address2,
            String zipCode,
            String city,
            String region,
            String countryCode,
            String phone,
            String company,
            String vatNumber,
            boolean isDefault
    ) {}
}
