package com.cyna.modules.user.interfaces.rest;

import com.cyna.modules.user.application.query.me.GetCurrentUserQuery;
import com.cyna.modules.user.application.query.me.UserReadModel;
import com.cyna.modules.user.interfaces.dto.response.UserResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
