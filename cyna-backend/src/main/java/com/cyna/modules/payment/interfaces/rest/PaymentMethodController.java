package com.cyna.modules.payment.interfaces.rest;

import com.cyna.modules.payment.application.command.savedpaymentmethod.*;
import com.cyna.modules.payment.application.query.listpaymentmethods.ListPaymentMethodsQuery;
import com.cyna.modules.payment.application.query.listpaymentmethods.SavedPaymentMethodReadModel;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account/payment-methods")
@Tag(name = "Payment Methods", description = "Saved card management")
public class PaymentMethodController {

    private final Mediator mediator;

    public PaymentMethodController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    @Operation(summary = "List saved payment methods for the authenticated user")
    public ResponseEntity<ApiResponse<List<SavedPaymentMethodReadModel>>> list(Authentication auth) {
        UUID userId = userId(auth);
        List<SavedPaymentMethodReadModel> methods = mediator.send(new ListPaymentMethodsQuery(userId));
        return ResponseEntity.ok(ApiResponse.success(methods));
    }

    @PostMapping("/setup-intent")
    @Operation(summary = "Create a Stripe SetupIntent so the frontend can collect card details")
    public ResponseEntity<ApiResponse<SetupIntentResponse>> createSetupIntent(Authentication auth) {
        UUID userId = userId(auth);
        var result = mediator.send(new CreateSetupIntentCommand(userId));

        return result.fold(
                clientSecret -> ResponseEntity.ok(ApiResponse.success(new SetupIntentResponse(clientSecret))),
                error -> switch (error) {
                    case "USER_NOT_FOUND" -> ResponseEntity.status(404)
                            .body(ApiResponse.error("USER_NOT_FOUND", null));
                    default -> error != null && error.startsWith("STRIPE_ERROR")
                            ? ResponseEntity.status(502).body(ApiResponse.error("STRIPE_ERROR", error))
                            : ResponseEntity.status(400).body(ApiResponse.error(error, null));
                }
        );
    }

    @PostMapping
    @Operation(summary = "Attach a confirmed Stripe PaymentMethod to the user's account")
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Detach and remove a saved payment method")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            Authentication auth) {
        UUID userId = userId(auth);
        var result = mediator.send(new DeletePaymentMethodCommand(id, userId));

        return result.fold(
                ignored -> ResponseEntity.noContent().build(),
                error -> switch (error) {
                    case "NOT_FOUND" -> ResponseEntity.status(404)
                            .body(ApiResponse.error("NOT_FOUND", null));
                    default -> error != null && error.startsWith("STRIPE_ERROR")
                            ? ResponseEntity.status(502).body(ApiResponse.error("STRIPE_ERROR", error))
                            : ResponseEntity.status(400).body(ApiResponse.error(error, null));
                }
        );
    }

    @PatchMapping("/{id}/default")
    @Operation(summary = "Set a saved payment method as default")
    public ResponseEntity<ApiResponse<Void>> setDefault(
            @PathVariable UUID id,
            Authentication auth) {
        UUID userId = userId(auth);
        var result = mediator.send(new SetDefaultPaymentMethodCommand(id, userId));

        return result.fold(
                ignored -> ResponseEntity.ok(ApiResponse.success(null)),
                error -> switch (error) {
                    case "NOT_FOUND" -> ResponseEntity.status(404)
                            .body(ApiResponse.error("NOT_FOUND", null));
                    default -> ResponseEntity.status(400).body(ApiResponse.error(error, null));
                }
        );
    }

    private UUID userId(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }

    record SetupIntentResponse(String clientSecret) {}
    record SaveRequest(@NotBlank String stripePaymentMethodId) {}
}
