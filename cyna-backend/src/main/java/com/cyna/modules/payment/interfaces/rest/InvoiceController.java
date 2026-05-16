package com.cyna.modules.payment.interfaces.rest;

import com.cyna.modules.payment.application.query.listinvoices.InvoiceReadModel;
import com.cyna.modules.payment.application.query.listinvoices.ListMyInvoicesQuery;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Billing history. Read-only relay of the user's Stripe invoices — Stripe
 * generates, numbers and retains the PDFs (legal 10-year obligation); we only
 * expose the signed links so the customer can download or print them.
 */
@RestController
@RequestMapping("/api/v1/account/invoices")
@Tag(name = "Invoices", description = "User billing history (Stripe-backed)")
public class InvoiceController {

    private final Mediator mediator;

    public InvoiceController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's invoices, most recent first")
    public ResponseEntity<ApiResponse<List<InvoiceReadModel>>> list(Authentication auth) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        List<InvoiceReadModel> invoices = mediator.send(new ListMyInvoicesQuery(userId));
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }
}
