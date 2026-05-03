package com.cyna.modules.payment.interfaces.rest;

import com.cyna.modules.payment.application.command.billingportal.OpenBillingPortalCommand;
import com.cyna.modules.payment.application.command.initiate.InitiatePaymentCommand;
import com.cyna.modules.payment.application.command.processwebhook.ProcessWebhookCommand;
import com.cyna.modules.payment.application.query.getbyid.GetPaymentByOrderIdQuery;
import com.cyna.modules.payment.interfaces.rest.dto.request.BillingPortalRequest;
import com.cyna.modules.payment.interfaces.rest.dto.request.InitiatePaymentRequest;
import com.cyna.modules.payment.interfaces.rest.dto.response.BillingPortalResponse;
import com.cyna.modules.payment.interfaces.rest.dto.response.PaymentIntentResponse;
import com.cyna.modules.payment.interfaces.rest.dto.response.PaymentResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing")
public class PaymentController {

    private final Mediator mediator;

    public PaymentController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment for an order")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> initiatePayment(
            @RequestBody @Valid InitiatePaymentRequest request,
            Authentication auth) {

        UUID userId = UUID.fromString((String) auth.getPrincipal());
        var result = mediator.send(new InitiatePaymentCommand(request.orderId(), userId));

        return result.fold(
                model -> ResponseEntity.ok(ApiResponse.success(PaymentIntentResponse.from(model))),
                error -> switch (error) {
                    case "ORDER_NOT_FOUND" -> ResponseEntity.status(404)
                            .body(ApiResponse.error("ORDER_NOT_FOUND", null));
                    case "ORDER_NOT_PAYABLE" -> ResponseEntity.status(409)
                            .body(ApiResponse.error("ORDER_NOT_PAYABLE", null));
                    case "MIXED_BILLING_CYCLES" -> ResponseEntity.status(422)
                            .body(ApiResponse.error("MIXED_BILLING_CYCLES", null));
                    case "USER_NOT_FOUND" -> ResponseEntity.status(404)
                            .body(ApiResponse.error("USER_NOT_FOUND", null));
                    default -> error != null && error.startsWith("STRIPE_ERROR")
                            ? ResponseEntity.status(502)
                                    .body(ApiResponse.error("STRIPE_ERROR", error))
                            : ResponseEntity.status(400)
                                    .body(ApiResponse.error(error, null));
                }
        );
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment status for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(
            @PathVariable UUID orderId,
            Authentication auth) {

        UUID userId = UUID.fromString((String) auth.getPrincipal());
        var model = mediator.send(new GetPaymentByOrderIdQuery(orderId, userId));

        if (model == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("PAYMENT_NOT_FOUND", null));
        }
        return ResponseEntity.ok(ApiResponse.success(PaymentResponse.from(model)));
    }

    @PostMapping("/billing-portal")
    @Operation(summary = "Create a Stripe Customer Portal session for the authenticated user")
    public ResponseEntity<ApiResponse<BillingPortalResponse>> openBillingPortal(
            @RequestBody @Valid BillingPortalRequest request,
            Authentication auth) {

        UUID userId = UUID.fromString((String) auth.getPrincipal());
        var result = mediator.send(new OpenBillingPortalCommand(userId, request.returnUrl()));

        return result.fold(
                url -> ResponseEntity.ok(ApiResponse.success(new BillingPortalResponse(url))),
                error -> switch (error) {
                    case "NO_STRIPE_CUSTOMER" -> ResponseEntity.status(404)
                            .body(ApiResponse.error("NO_STRIPE_CUSTOMER", null));
                    default -> error != null && error.startsWith("STRIPE_ERROR")
                            ? ResponseEntity.status(502)
                                    .body(ApiResponse.error("STRIPE_ERROR", error))
                            : ResponseEntity.status(400)
                                    .body(ApiResponse.error(error, null));
                }
        );
    }

    @PostMapping("/webhook")
    @Operation(summary = "Stripe webhook endpoint (public)")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        if (sigHeader == null || sigHeader.isBlank()) {
            return ResponseEntity.status(400).build();
        }

        var result = mediator.send(new ProcessWebhookCommand(payload, sigHeader));

        return result.isSuccess()
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(400).build();
    }
}
