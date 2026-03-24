package com.cyna.modules.user.interfaces.rest;

import com.cyna.modules.user.application.command.login.LoginCommand;
import com.cyna.modules.user.application.command.logout.LogoutCommand;
import com.cyna.modules.user.application.command.refresh.RefreshTokenCommand;
import com.cyna.modules.user.application.command.register.RegisterUserCommand;
import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.interfaces.dto.request.LoginRequest;
import com.cyna.modules.user.interfaces.dto.request.RefreshRequest;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import com.cyna.modules.user.interfaces.dto.response.AuthResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, token refresh and logout")
public class AuthController {

    private final Mediator mediator;

    public AuthController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "Register a new user", description = "Creates an account and returns JWT tokens")
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business rule violation (e.g. email already taken)")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        var command = new RegisterUserCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.lang() != null ? request.lang() : "fr"
        );

        Result<AuthTokens> result = mediator.send(command);

        return result.fold(
                tokens -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(AuthResponse.from(
                                tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn()
                        ))),
                error -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error))
        );
    }

    @Operation(summary = "Login", description = "Authenticates credentials and returns JWT tokens")
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        var command = new LoginCommand(request.email(), request.password());

        Result<AuthTokens> result = mediator.send(command);

        return result.fold(
                tokens -> ResponseEntity.ok(ApiResponse.success(AuthResponse.from(
                        tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn()
                ))),
                error -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("UNAUTHORIZED", error))
        );
    }

    @Operation(summary = "Refresh access token", description = "Issues a new access token using a valid refresh token")
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        var command = new RefreshTokenCommand(request.refreshToken());

        Result<AuthTokens> result = mediator.send(command);

        return result.fold(
                tokens -> ResponseEntity.ok(ApiResponse.success(AuthResponse.from(
                        tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn()
                ))),
                error -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("UNAUTHORIZED", error))
        );
    }

    @Operation(summary = "Logout", description = "Revokes the provided refresh token")
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        var command = new LogoutCommand(request.refreshToken());

        Result<Void> result = mediator.send(command);

        return result.fold(
                success -> ResponseEntity.ok(ApiResponse.<Void>success(null)),
                error -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("UNAUTHORIZED", error))
        );
    }
}
