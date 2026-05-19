package com.cyna.shared.interfaces.rest;

import com.cyna.shared.application.notification.MailService;
import com.cyna.shared.interfaces.dto.request.ContactRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contact")
@Tag(name = "Contact", description = "Public contact form endpoint")
public class ContactController {

    private final MailService mailService;

    public ContactController(MailService mailService) {
        this.mailService = mailService;
    }

    @Operation(
            summary = "Submit a contact form",
            description = "Sends a contact email to the configured internal address via Brevo. No authentication required."
    )
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitContactForm(@Valid @RequestBody ContactRequest request) {
        String lang = request.lang() != null ? request.lang() : "fr";
        mailService.sendContactEmail(
                request.email(),
                request.name(),
                request.subject(),
                request.message(),
                lang
        );
        mailService.sendContactAcknowledgementEmail(
                request.email(),
                request.name(),
                request.subject(),
                lang
        );
        return ResponseEntity.ok(ApiResponse.<Void>success(null));
    }
}
