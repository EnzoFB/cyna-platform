package com.cyna.modules.user.interfaces.rest;

import com.cyna.modules.user.application.command.login.LoginCommand;
import com.cyna.modules.user.application.command.login.LoginCommandHandler;
import com.cyna.modules.user.application.command.login.verifyotp.VerifyLoginOtpCommand;
import com.cyna.modules.user.application.command.logout.LogoutCommand;
import com.cyna.modules.user.application.command.passwordreset.RequestPasswordResetCommand;
import com.cyna.modules.user.application.command.passwordreset.ResetPasswordCommand;
import com.cyna.modules.user.application.command.refresh.RefreshTokenCommand;
import com.cyna.modules.user.application.command.register.RegisterUserCommand;
import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.modules.user.application.model.LoginChallenge;
import com.cyna.modules.user.domain.model.Role;
import com.cyna.modules.user.interfaces.dto.request.ForgotPasswordRequest;
import com.cyna.modules.user.interfaces.dto.request.LoginRequest;
import com.cyna.modules.user.interfaces.dto.request.RefreshRequest;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import com.cyna.modules.user.interfaces.dto.request.ResetPasswordRequest;
import com.cyna.modules.user.interfaces.dto.request.VerifyLoginOtpRequest;
import com.cyna.modules.user.interfaces.dto.response.AuthResponse;
import com.cyna.modules.user.interfaces.dto.response.LoginChallengeResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(summary = "Login", description = "Authenticates credentials and starts OTP challenge")
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Credentials valid, OTP challenge created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many OTP requests for this account")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginChallengeResponse>> login(@Valid @RequestBody LoginRequest request) {
        var command = new LoginCommand(request.email(), request.password(), null, request.lang());

        Result<LoginChallenge> result = mediator.send(command);

        return result.fold(
                challenge -> ResponseEntity.ok(ApiResponse.success(LoginChallengeResponse.from(challenge))),
                error -> {
                    if (LoginCommandHandler.TOO_MANY_OTP_REQUESTS.equals(error)) {
                        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .body(ApiResponse.error("TOO_MANY_OTP_REQUESTS", error));
                    }
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error("UNAUTHORIZED", error));
                }
        );
    }

    @Operation(summary = "Verify login OTP", description = "Validates OTP challenge and returns JWT tokens")
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP valid, login finalized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired OTP")
    })
    @PostMapping("/login/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyLoginOtp(@Valid @RequestBody VerifyLoginOtpRequest request) {
        var command = new VerifyLoginOtpCommand(request.challengeId(), request.otpCode());

        Result<AuthTokens> result = mediator.send(command);

        return result.fold(
                tokens -> ResponseEntity.ok(ApiResponse.success(AuthResponse.from(
                        tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn()
                ))),
                error -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("UNAUTHORIZED", error))
        );
    }

    @Operation(summary = "Admin login", description = "Authenticates admin credentials and starts OTP challenge")
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Credentials valid, OTP challenge created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<LoginChallengeResponse>> adminLogin(@Valid @RequestBody LoginRequest request) {
        var command = new LoginCommand(request.email(), request.password(), Role.ADMIN.name(), "fr");

        Result<LoginChallenge> result = mediator.send(command);

        return result.fold(
                challenge -> ResponseEntity.ok(ApiResponse.success(LoginChallengeResponse.from(challenge))),
                error -> {
                    if (LoginCommandHandler.ACCESS_DENIED.equals(error)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error("ACCESS_DENIED", error));
                    }
                    if (LoginCommandHandler.TOO_MANY_OTP_REQUESTS.equals(error)) {
                        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .body(ApiResponse.error("TOO_MANY_OTP_REQUESTS", error));
                    }
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error("UNAUTHORIZED", error));
                }
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

    @Operation(
            summary = "Request a password reset",
            description = "Triggers a one-shot reset link sent to the registered email. Always returns "
                    + "200 regardless of whether the email is registered, to prevent address enumeration."
    )
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request accepted")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        var command = new RequestPasswordResetCommand(request.email(), request.lang());

        mediator.send(command);

        // Always 200, even if the email is unknown — see anti-enumeration note above.
        return ResponseEntity.ok(ApiResponse.<Void>success(null));
    }

    @Operation(
            summary = "Reset password with a one-shot token",
            description = "Sets a new password using the token sent by email. On success, every active "
                    + "session for the user is revoked; the user must log back in."
    )
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        var command = new ResetPasswordCommand(request.token(), request.newPassword());

        Result<Void> result = mediator.send(command);

        return result.fold(
                success -> ResponseEntity.ok(ApiResponse.<Void>success(null)),
                error   -> ResponseEntity.badRequest()
                                         .body(ApiResponse.error("INVALID_TOKEN", error))
        );
    }

    @Operation(
            summary = "Logout",
            description = "Revokes the provided refresh token. With ?allDevices=true, revokes every "
                    + "active session for the owning user."
    )
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshRequest request,
            @RequestParam(value = "allDevices", defaultValue = "false") boolean allDevices) {

        var command = new LogoutCommand(request.refreshToken(), allDevices);

        Result<Void> result = mediator.send(command);

        return result.fold(
                success -> ResponseEntity.ok(ApiResponse.<Void>success(null)),
                error -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("UNAUTHORIZED", error))
        );
    }
}
