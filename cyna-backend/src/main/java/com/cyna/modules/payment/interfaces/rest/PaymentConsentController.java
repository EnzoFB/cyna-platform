package com.cyna.modules.payment.interfaces.rest;

import com.cyna.modules.payment.application.command.consentlog.LogPaymentConsentCommand;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * GDPR Article 7.1 proof-of-consent endpoint. The frontend calls this
 * immediately after a {@code SavePaymentMethodCommand} succeeded at
 * checkout when the user ticked the "Reuse this card" checkbox.
 *
 * <p>IP address and User-Agent are read from the request server-side so the
 * user cannot tamper with them.
 */
@RestController
@RequestMapping("/api/v1/account/consent-log")
@Tag(name = "Payment consent log", description = "GDPR proof-of-consent recording")
public class PaymentConsentController {

    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final Mediator mediator;

    public PaymentConsentController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/payment-method")
    @Operation(summary = "Record payment-method consent",
            description = "Records GDPR Art. 7.1 proof-of-consent for persisting a payment method. IP and User-Agent "
                    + "are captured server-side and cannot be supplied by the client. Returns 201 on success.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Consent recorded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication")
    })
    public ResponseEntity<ApiResponse<Void>> logPaymentMethodConsent(
            @RequestBody @Valid PaymentMethodConsentRequest request,
            HttpServletRequest httpRequest,
            Authentication auth) {

        UUID userId = UUID.fromString((String) auth.getPrincipal());

        mediator.send(new LogPaymentConsentCommand(
                userId,
                request.labelVersion(),
                request.stripePaymentMethodId(),
                clientIp(httpRequest),
                truncate(httpRequest.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH)
        ));

        return ResponseEntity.status(201).body(ApiResponse.success(null));
    }

    /**
     * Returns the original client IP — honors the leftmost entry in
     * {@code X-Forwarded-For} when present (we sit behind a reverse proxy in
     * prod), otherwise the direct socket address.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma == -1 ? forwarded : forwarded.substring(0, comma);
            return first.trim();
        }
        return request.getRemoteAddr();
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }

    public record PaymentMethodConsentRequest(
            @NotBlank @Size(max = 255) String stripePaymentMethodId,
            @NotBlank @Size(max = 32) String labelVersion
    ) {}
}
