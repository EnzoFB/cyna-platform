package com.cyna.modules.user.interfaces.rest;

import com.cyna.modules.user.application.command.create.CreateAdminUserCommand;
import com.cyna.modules.user.application.command.update.UpdateUserCommand;
import com.cyna.modules.user.application.query.list.GetUsersQuery;
import com.cyna.modules.user.application.query.list.UsersPage;
import com.cyna.modules.user.interfaces.dto.request.CreateAdminUserRequest;
import com.cyna.modules.user.interfaces.dto.request.UpdateUserRequest;
import com.cyna.modules.user.interfaces.dto.response.AdminUserResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import com.cyna.shared.interfaces.rest.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Admin operations on user accounts")
public class AdminUserController {

    private final Mediator mediator;

    public AdminUserController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "List all users", description = "Returns a paginated list of all registered users")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int effectiveSize = Math.min(size, 100);
        UsersPage result = mediator.send(new GetUsersQuery(page, effectiveSize));

        List<AdminUserResponse> responses = result.items().stream()
                .map(AdminUserResponse::from)
                .toList();

        PagedResponse<AdminUserResponse> paged = PagedResponse.of(responses, page, effectiveSize, result.totalElements());

        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @Operation(summary = "Create a user", description = "Creates a new user with the specified role")
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, UUID>>> createUser(
            @Valid @RequestBody CreateAdminUserRequest request) {

        var command = new CreateAdminUserCommand(
                request.email(), request.password(),
                request.firstName(), request.lastName(), request.role()
        );

        return mediator.send(command).fold(
                userId -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(Map.of("id", userId))),
                error -> ResponseEntity.badRequest()
                        .body(ApiResponse.error("CREATE_FAILED", error))
        );
    }

    @Operation(summary = "Update a user", description = "Updates user information")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        var command = new UpdateUserCommand(
                id, request.firstName(), request.lastName(),
                request.role(), request.status()
        );

        return mediator.send(command).fold(
                v -> ResponseEntity.ok(ApiResponse.<Void>success(null)),
                error -> ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>error("UPDATE_FAILED", error))
        );
    }
}
