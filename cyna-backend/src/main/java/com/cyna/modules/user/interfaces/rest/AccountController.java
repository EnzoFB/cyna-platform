package com.cyna.modules.user.interfaces.rest;

import com.cyna.modules.user.application.command.emailchange.ConfirmEmailChangeCommand;
import com.cyna.modules.user.application.command.emailchange.RequestEmailChangeCommand;
import com.cyna.modules.user.application.command.profile.UpdateProfileCommand;
import com.cyna.modules.user.application.query.email.CheckEmailQuery;
import com.cyna.modules.user.application.query.me.GetCurrentUserQuery;
import com.cyna.modules.user.application.query.me.UserReadModel;
import com.cyna.modules.user.interfaces.dto.request.RequestEmailChangeRequest;
import com.cyna.modules.user.interfaces.dto.request.UpdateProfileRequest;
import com.cyna.modules.user.interfaces.dto.response.UserResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account", description = "Authenticated user account operations")
public class AccountController {

    private final Mediator mediator;

    public AccountController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "Get current user", description = "Returns the profile of the authenticated user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal String userId) {
        var query = new GetCurrentUserQuery(UUID.fromString(userId));

        UserReadModel user = mediator.send(query);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        var query = new CheckEmailQuery(email);
        boolean exists = mediator.send(query);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    @Operation(summary = "Update profile", description = "Updates firstName, lastName and/or company of the authenticated user")
    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody UpdateProfileRequest request) {

        var command = new UpdateProfileCommand(
                UUID.fromString(userId),
                request.firstName(),
                request.lastName(),
                request.company()
        );

        Result<Void> result = mediator.send(command);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ERROR", result.getError()));
        }

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Request email change", description = "Sends a confirmation email to the new address")
    @PostMapping("/email/request-change")
    public ResponseEntity<ApiResponse<Void>> requestEmailChange(
            @AuthenticationPrincipal String userId,
            @RequestBody RequestEmailChangeRequest request) {

        var command = new RequestEmailChangeCommand(
                UUID.fromString(userId),
                request.newEmail(),
                request.lang() != null ? request.lang() : "fr"
        );

        Result<Void> result = mediator.send(command);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ERROR", result.getError()));
        }

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Confirm email change", description = "Confirms the new email address using the token from the confirmation email")
    @PostMapping("/email/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailChange(@RequestParam String token) {
        var command = new ConfirmEmailChangeCommand(token);

        Result<Void> result = mediator.send(command);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ERROR", result.getError()));
        }

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
