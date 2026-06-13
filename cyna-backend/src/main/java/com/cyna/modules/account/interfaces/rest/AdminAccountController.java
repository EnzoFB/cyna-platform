package com.cyna.modules.account.interfaces.rest;

import com.cyna.modules.account.application.command.delete.DeleteUserCommand;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-initiated RGPD Art. 17 erasure ({@code DELETE /api/v1/admin/users/{id}}).
 * Lives in the {@code account} module because erasure spans several bounded
 * contexts; the rest of the admin user CRUD stays in the user module.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Admin operations on user accounts")
public class AdminAccountController {

    private final Mediator mediator;

    public AdminAccountController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "Delete a user", description = "Permanently erases a user account (RGPD Art. 17)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {

        return mediator.send(new DeleteUserCommand(id)).fold(
                v -> ResponseEntity.ok(ApiResponse.<Void>success(null)),
                error -> ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>error("DELETE_FAILED", error))
        );
    }
}
