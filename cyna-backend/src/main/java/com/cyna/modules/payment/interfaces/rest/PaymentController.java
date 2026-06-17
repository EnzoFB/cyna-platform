package com.cyna.modules.payment.interfaces.rest;

import com.cyna.modules.payment.application.command.billingportal.OpenBillingPortalCommand;
import com.cyna.modules.payment.application.command.finalize.FinalizePaymentCommand;
import com.cyna.modules.payment.application.command.initiate.InitiatePaymentCommand;
import com.cyna.modules.payment.application.command.processwebhook.ProcessWebhookCommand;
import com.cyna.modules.payment.application.api.PaymentQueryApi;
import com.cyna.modules.payment.application.query.getbyid.GetPaymentByOrderIdQuery;
import com.cyna.modules.payment.application.query.previewtax.PreviewTaxQuery;
import com.cyna.modules.payment.interfaces.rest.dto.response.OrderTaxSummaryResponse;
import com.cyna.modules.payment.interfaces.rest.dto.request.BillingPortalRequest;
import com.cyna.modules.payment.interfaces.rest.dto.request.FinalizePaymentRequest;
import com.cyna.modules.payment.interfaces.rest.dto.request.InitiatePaymentRequest;
import com.cyna.modules.payment.interfaces.rest.dto.request.TaxPreviewRequest;
import com.cyna.modules.payment.interfaces.rest.dto.response.BillingPortalResponse;
import com.cyna.modules.payment.interfaces.rest.dto.response.FinalizePaymentResponse;
import com.cyna.modules.payment.interfaces.rest.dto.response.PaymentIntentResponse;
import com.cyna.modules.payment.interfaces.rest.dto.response.PaymentResponse;
import com.cyna.modules.payment.interfaces.rest.dto.response.TaxPreviewResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
    private final PaymentQueryApi paymentQueryApi;

    public PaymentController(Mediator mediator, PaymentQueryApi paymentQueryApi) {
        this.mediator = mediator;
        this.paymentQueryApi = paymentQueryApi;
    }

    @PostMapping("/initiate")
    @Operation(summary = "Initiate checkout",
            description = "Creates a Stripe SetupIntent for the given order and returns its client secret so the "
                    + "frontend can collect a payment method. The order must belong to the authenticated user and be payable.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SetupIntent created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND or USER_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "ORDER_NOT_PAYABLE — the order is not in a payable state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "STRIPE_ERROR — upstream Stripe failure")
    })
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
    @Operation(summary = "Finalize checkout",
            description = "Confirms the collected payment method and creates one Stripe Subscription per order line. "
                    + "On a declined card the payment is left FAILED so the customer can retry with another card.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment finalized; subscriptions created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "402", description = "PAYMENT_DECLINED — the card was declined; retry with another card"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The order belongs to another user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "PAYMENT_NOT_INITIATED, PAYMENT_NOT_FINALIZABLE or NO_STRIPE_CUSTOMER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "STRIPE_ERROR — upstream Stripe failure")
    })
    public ResponseEntity<ApiResponse<FinalizePaymentResponse>> finalizePayment(
            @RequestBody @Valid FinalizePaymentRequest request,
            Authentication auth) {

        UUID userId = UUID.fromString((String) auth.getPrincipal());
        var result = mediator.send(new FinalizePaymentCommand(
                request.orderId(), userId, request.paymentMethodId(), request.vatNumber()));

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

    @PostMapping("/tax-preview")
    @Operation(summary = "Preview VAT for a prospective checkout",
            description = "Computes the exact VAT (including B2B reverse charge) for a set of lines and a billing "
                    + "location, before any order exists. Prices are resolved server-side from the product ids.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tax preview computed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication")
    })
    public ResponseEntity<ApiResponse<TaxPreviewResponse>> previewTax(
            @RequestBody @Valid TaxPreviewRequest request,
            Authentication auth) {

        // Authenticated like the rest of checkout, but the calculation itself is
        // user-agnostic (prices resolved from productId, location from the body).
        var query = new PreviewTaxQuery(
                request.currency(),
                request.lines().stream()
                        .map(l -> new PreviewTaxQuery.Line(l.productId(), l.billingCycle(), l.quantity()))
                        .toList(),
                request.countryCode(),
                request.postalCode(),
                request.state(),
                request.vatNumber());

        var model = mediator.send(query);
        return ResponseEntity.ok(ApiResponse.success(TaxPreviewResponse.from(model)));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment status for an order",
            description = "Returns the payment record for an order owned by the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PAYMENT_NOT_FOUND — no payment for this order/user")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(
            @Parameter(description = "Id of the order to read the payment for") @PathVariable UUID orderId,
            Authentication auth) {

        UUID userId = UUID.fromString((String) auth.getPrincipal());
        var model = mediator.send(new GetPaymentByOrderIdQuery(orderId, userId));

        if (model == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("PAYMENT_NOT_FOUND", null));
        }
        return ResponseEntity.ok(ApiResponse.success(PaymentResponse.from(model)));
    }

    @GetMapping("/order/{orderId}/tax-summary")
    @Operation(summary = "Authoritative VAT/TTC for an order",
            description = "Returns the definitive VAT and total-including-tax for an order, read back from its "
                    + "Stripe invoices (used on the confirmation page and email).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tax summary returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order or its invoices not found")
    })
    public ResponseEntity<ApiResponse<OrderTaxSummaryResponse>> getOrderTaxSummary(
            @Parameter(description = "Id of the order to summarize tax for") @PathVariable UUID orderId,
            Authentication auth) {

        UUID userId = UUID.fromString((String) auth.getPrincipal());
        var view = paymentQueryApi.getOrderTaxSummary(userId, orderId);
        return ResponseEntity.ok(ApiResponse.success(OrderTaxSummaryResponse.from(view)));
    }

    @PostMapping("/billing-portal")
    @Operation(summary = "Open the Stripe billing portal",
            description = "Creates a Stripe Customer Portal session for the authenticated user and returns the URL "
                    + "to redirect them to, with the supplied return URL.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Portal session created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NO_STRIPE_CUSTOMER — the user has no Stripe customer yet"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "STRIPE_ERROR — upstream Stripe failure")
    })
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
    @Operation(summary = "Stripe webhook receiver",
            description = "Public endpoint called by Stripe. The raw request body is verified against the "
                    + "`Stripe-Signature` header before processing; it is not called by API clients.")
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid Stripe signature")
    })
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
