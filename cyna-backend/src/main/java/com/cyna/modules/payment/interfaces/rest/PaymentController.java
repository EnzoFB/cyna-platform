package com.cyna.modules.payment.interfaces.rest;

import com.cyna.modules.payment.application.command.billingportal.OpenBillingPortalCommand;
import com.cyna.modules.payment.application.command.finalize.FinalizePaymentCommand;
import com.cyna.modules.payment.application.command.initiate.InitiatePaymentCommand;
import com.cyna.modules.payment.application.command.processwebhook.ProcessWebhookCommand;
import com.cyna.modules.payment.application.query.getbyid.GetPaymentByOrderIdQuery;
import com.cyna.modules.payment.interfaces.rest.dto.request.BillingPortalRequest;
import com.cyna.modules.payment.interfaces.rest.dto.request.FinalizePaymentRequest;
import com.cyna.modules.payment.interfaces.rest.dto.request.InitiatePaymentRequest;
import com.cyna.modules.payment.interfaces.rest.dto.response.BillingPortalResponse;
import com.cyna.modules.payment.interfaces.rest.dto.response.FinalizePaymentResponse;
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

import java.nio.charset.StandardCharsets;
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
    @Operation(summary = "Initiate checkout — creates a Stripe SetupIntent for the order")
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

    @PostMapping("/finalize")
    @Operation(summary = "Finalize checkout — creates one Stripe Subscription per OrderLine")
    public ResponseEntity<ApiResponse<FinalizePaymentResponse>> finalizePayment(
            @RequestBody @Valid FinalizePaymentRequest request,
            Authentication auth) {

        UUID userId = UUID.fromString((String) auth.getPrincipal());
        var result = mediator.send(new FinalizePaymentCommand(
                request.orderId(), userId, request.paymentMethodId()));

        return result.fold(
                model -> ResponseEntity.ok(ApiResponse.success(FinalizePaymentResponse.from(model))),
                error -> switch (error) {
                    case "ORDER_NOT_FOUND" -> ResponseEntity.status(404)
                            .body(ApiResponse.error("ORDER_NOT_FOUND", null));
                    case "PAYMENT_NOT_INITIATED" -> ResponseEntity.status(409)
                            .body(ApiResponse.error("PAYMENT_NOT_INITIATED", null));
                    case "PAYMENT_NOT_FINALIZABLE" -> ResponseEntity.status(409)
                            .body(ApiResponse.error("PAYMENT_NOT_FINALIZABLE", null));
                    // 402 Payment Required — the card was declined. The Payment
                    // is left FAILED so the customer can retry with another card.
                    case "PAYMENT_DECLINED" -> ResponseEntity.status(402)
                            .body(ApiResponse.error("PAYMENT_DECLINED", null));
                    case "Access denied" -> ResponseEntity.status(403)
                            .body(ApiResponse.error("FORBIDDEN", null));
                    case "NO_STRIPE_CUSTOMER" -> ResponseEntity.status(409)
                            .body(ApiResponse.error("NO_STRIPE_CUSTOMER", null));
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
            // Receive the raw body as bytes to guarantee a byte-for-byte match
            // with what Stripe signed. @RequestBody String would route through
            // Spring's StringHttpMessageConverter which can apply charset
            // normalization that invalidates the HMAC signature.
            @RequestBody byte[] payloadBytes,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        if (sigHeader == null || sigHeader.isBlank()) {
            return ResponseEntity.status(400).build();
        }

        // Stripe payloads are always UTF-8 ; converting back to String at the
        // boundary is a no-op for the signed bytes.
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        var result = mediator.send(new ProcessWebhookCommand(payload, sigHeader));

        return result.isSuccess()
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(400).build();
    }
}
