package com.cyna.modules.payment.interfaces.rest;

import com.cyna.modules.payment.application.command.savedpaymentmethod.SavePaymentMethodCommand;
import com.cyna.modules.payment.application.query.listpaymentmethods.ListPaymentMethodsQuery;
import com.cyna.modules.payment.application.query.listpaymentmethods.SavedPaymentMethodReadModel;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Read-mostly API for saved payment methods. Everything beyond "list + persist
 * a card collected during checkout" is delegated to the Stripe Customer Portal
 * (delete, set default, update expired, view invoices, manage subscriptions).
 *
 * <p>The portal session is opened via {@code POST /payments/billing-portal}
 * on {@link PaymentController}.
 */
@RestController
@RequestMapping("/api/v1/account/payment-methods")
@Tag(name = "Payment Methods", description = "Saved card listing + checkout-side persistence")
public class PaymentMethodController {

    private final Mediator mediator;

    public PaymentMethodController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    @Operation(summary = "List saved payment methods",
            description = "Returns the cards the authenticated user has saved for reuse at checkout.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Saved methods returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication")
    })
    public ResponseEntity<ApiResponse<List<SavedPaymentMethodReadModel>>> list(Authentication auth) {
        UUID userId = userId(auth);
        List<SavedPaymentMethodReadModel> methods = mediator.send(new ListPaymentMethodsQuery(userId));
        return ResponseEntity.ok(ApiResponse.success(methods));
    }

    @PostMapping
    @Operation(summary = "Save a payment method",
            description = "Persists a Stripe PaymentMethod confirmed during checkout, after the user gave explicit "
                    + "consent. Returns 201 on success.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment method saved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NO_STRIPE_CUSTOMER — the user has no Stripe customer yet"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "STRIPE_ERROR — upstream Stripe failure")
    })
    public ResponseEntity<ApiResponse<Void>> save(
            @RequestBody @Valid SaveRequest request,
            Authentication auth) {
        UUID userId = userId(auth);
        var result = mediator.send(new SavePaymentMethodCommand(userId, request.stripePaymentMethodId()));

        return result.fold(
                ignored -> ResponseEntity.status(201).body(ApiResponse.success(null)),
                error -> switch (error) {
                    case "NO_STRIPE_CUSTOMER" -> ResponseEntity.status(404)
                            .body(ApiResponse.error("NO_STRIPE_CUSTOMER", null));
                    default -> error != null && error.startsWith("STRIPE_ERROR")
                            ? ResponseEntity.status(502).body(ApiResponse.error("STRIPE_ERROR", error))
                            : ResponseEntity.status(400).body(ApiResponse.error(error, null));
                }
        );
    }

    private UUID userId(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }

    record SaveRequest(@NotBlank String stripePaymentMethodId) {}
}
