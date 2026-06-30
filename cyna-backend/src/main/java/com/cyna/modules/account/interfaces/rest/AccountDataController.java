package com.cyna.modules.account.interfaces.rest;

import com.cyna.modules.account.application.command.delete.DeleteMyAccountCommand;
import com.cyna.modules.account.application.query.export.ExportMyDataQuery;
import com.cyna.modules.account.application.query.export.MyDataExport;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * RGPD self-service data rights for the authenticated user: export (Art. 15/20)
 * and erasure (Art. 17). Lives in the {@code account} orchestration module
 * because both span several bounded contexts (user, order, subscription,
 * payment).
 */
@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account data (RGPD)", description = "Personal-data export and account erasure")
public class AccountDataController {

    private final Mediator mediator;

    public AccountDataController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(
            summary = "Export my data (RGPD Art. 15/20)",
            description = "Returns all personal data held about the authenticated user as a "
                    + "downloadable, machine-readable JSON file."
    )
    @GetMapping("/export")
    public ResponseEntity<MyDataExport> exportMyData(@AuthenticationPrincipal String userId) {
        MyDataExport export = mediator.send(new ExportMyDataQuery(UUID.fromString(userId)));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"cyna-my-data.json\"")
                .body(export);
    }

    @Operation(
            summary = "Delete my account (RGPD Art. 17)",
            description = "Erases the authenticated user's account. If the account has a "
                    + "legally-retained transactional footprint (orders/invoices) it is "
                    + "irreversibly anonymized and access is revoked; otherwise it is fully "
                    + "deleted. The operation cannot be undone."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account erased"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(@AuthenticationPrincipal String userId) {
        return mediator.send(new DeleteMyAccountCommand(UUID.fromString(userId))).fold(
                v -> ResponseEntity.ok(ApiResponse.<Void>success(null)),
                error -> ResponseEntity.badRequest().body(ApiResponse.<Void>error("ERROR", error))
        );
    }
}
