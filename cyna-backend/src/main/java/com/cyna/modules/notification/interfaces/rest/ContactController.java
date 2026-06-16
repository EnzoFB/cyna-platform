package com.cyna.modules.notification.interfaces.rest;

import com.cyna.modules.notification.application.NotificationDispatcher;
import com.cyna.shared.interfaces.dto.request.ContactRequest;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public contact-form endpoint. A contact submission <em>is</em> a notification
 * (send-an-email-to-the-team), so it now lives in the notification module rather
 * than the shared kernel. Same path ({@code /api/v1/contact}) — no client change.
 */
@RestController
@RequestMapping("/api/v1/contact")
@Tag(name = "Contact", description = "Public contact form endpoint")
public class ContactController {

    private final NotificationDispatcher notificationDispatcher;

    public ContactController(NotificationDispatcher notificationDispatcher) {
        this.notificationDispatcher = notificationDispatcher;
    }

    @Operation(
            summary = "Submit a contact form",
            description = "Sends a contact email to the configured internal address via Brevo. No authentication required."
    )
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitContactForm(@Valid @RequestBody ContactRequest request) {
        String lang = request.lang() != null ? request.lang() : "fr";
        notificationDispatcher.sendContactEmail(
                request.email(),
                request.name(),
                request.subject(),
                request.message(),
                lang
        );
        notificationDispatcher.sendContactAcknowledgementEmail(
                request.email(),
                request.name(),
                request.subject(),
                lang
        );
        return ResponseEntity.ok(ApiResponse.<Void>success(null));
    }
}
