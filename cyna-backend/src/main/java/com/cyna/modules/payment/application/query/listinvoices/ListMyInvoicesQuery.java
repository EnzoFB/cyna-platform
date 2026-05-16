package com.cyna.modules.payment.application.query.listinvoices;

import com.cyna.shared.application.Query;

import java.util.List;
import java.util.UUID;

/**
 * Lists the authenticated user's Stripe invoices (billing history). Stripe is
 * the system of record — we never persist invoice content locally, we only
 * relay the signed PDF / hosted links so the customer can fulfil the legal
 * requirement of accessing their invoices.
 */
public record ListMyInvoicesQuery(UUID userId) implements Query<List<InvoiceReadModel>> {}
