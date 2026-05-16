package com.cyna.modules.payment.application.query.listinvoices;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceReadModel(
        String id,
        String number,
        String status,
        BigDecimal amountPaid,
        String currency,
        Instant createdAt,
        String hostedInvoiceUrl,
        String invoicePdfUrl
) {}
