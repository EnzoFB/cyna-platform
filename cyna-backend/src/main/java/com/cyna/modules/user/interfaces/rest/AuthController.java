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
import com.cyna.modules.user.application.model.LoginOutcome;
import com.cyna.modules.user.application.model.VerifyOtpOutcome;
import com.cyna.modules.user.domain.model.Role;
import com.cyna.modules.user.infrastructure.security.RefreshCookieService;
import com.cyna.modules.user.infrastructure.security.TrustedDeviceCookieService;
import com.cyna.modules.user.interfaces.dto.request.ForgotPasswordRequest;
import com.cyna.modules.user.interfaces.dto.request.LoginRequest;
import com.cyna.modules.user.interfaces.dto.request.RefreshRequest;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import com.cyna.modules.user.interfaces.dto.request.ResetPasswordRequest;
import com.cyna.modules.user.interfaces.dto.request.VerifyLoginOtpRequest;
import com.cyna.modules.user.interfaces.dto.response.AuthResponse;
import com.cyna.modules.user.interfaces.dto.response.LoginResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
    private final RefreshCookieService refreshCookieService;
    private final TrustedDeviceCookieService trustedDeviceCookieService;

    public AuthController(Mediator mediator,
                          RefreshCookieService refreshCookieService,
                          TrustedDeviceCookieService trustedDeviceCookieService) {
        this.mediator = mediator;
        this.refreshCookieService = refreshCookieService;
        this.trustedDeviceCookieService = trustedDeviceCookieService;
    }

    @Operation(summary = "Register a new user", description = "Creates an account and returns JWT tokens")
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business rule violation (e.g. email already taken)")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        var command = new RegisterUserCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.company(),
                request.lang() != null ? request.lang() : "fr",
                request.acceptTerms(),
                clientIp(httpRequest),
                truncate(httpRequest.getHeader("User-Agent"), 512)
        );

        Result<AuthTokens> result = mediator.send(command);

        return result.fold(
                tokens -> ResponseEntity.status(HttpStatus.CREATED)
                        .headers(refreshCookieService.cookieHeaders(
                                refreshCookieService.issueCookieHeader(tokens.refreshToken())))
                        .body(ApiResponse.success(AuthResponse.from(
                                tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn()
                        ))),
                error -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error))
        );
    }

    @Operation(
            summary = "Login",
            description = "Validates credentials. If the request carries a still-valid device_token "
                    + "cookie (browser previously OTP-verified), emits access + refresh tokens directly "
                    + "(skip OTP). Otherwise creates an OTP challenge and returns its id."
    )
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Credentials valid — either tokens or OTP challenge"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many OTP requests for this account")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            HttpServletRequest httpRequest,
            @Valid @RequestBody LoginRequest request) {
        return handleLogin(httpRequest, request.email(), request.password(), null, request.lang());
    }

    @Operation(summary = "Verify login OTP", description = "Validates OTP challenge and returns JWT tokens")
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP valid, login finalized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired OTP")
    })
    @PostMapping("/login/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyLoginOtp(
            HttpServletRequest httpRequest,
            @Valid @RequestBody VerifyLoginOtpRequest request) {
        var command = new VerifyLoginOtpCommand(
                request.challengeId(),
                request.otpCode(),
                httpRequest.getHeader("User-Agent")
        );

        Result<VerifyOtpOutcome> result = mediator.send(command);

        return result.fold(
                outcome -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.SET_COOKIE,
                            refreshCookieService.issueCookieHeader(outcome.tokens().refreshToken()));
                    headers.add(HttpHeaders.SET_COOKIE,
                            trustedDeviceCookieService.issueCookieHeader(outcome.trustedDeviceToken()));
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(ApiResponse.success(AuthResponse.from(
                                    outcome.tokens().accessToken(),
                                    outcome.tokens().refreshToken(),
                                    outcome.tokens().expiresIn()
                            )));
                },
                error -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("UNAUTHORIZED", error))
        );
    }

    @Operation(
            summary = "Admin login",
            description = "Same as /login but enforces ADMIN role. Trusted-device fast-path applies."
    )
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Credentials valid — either tokens or OTP challenge"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(
            HttpServletRequest httpRequest,
            @Valid @RequestBody LoginRequest request) {
        return handleLogin(httpRequest, request.email(), request.password(), Role.ADMIN.name(), "fr");
    }

    @Operation(
            summary = "Refresh access token",
            description = "Reads the refresh token from the refresh_token cookie (preferred) or "
                    + "the JSON body for the legacy deprecation window. Returns a new access "
                    + "token + rotates the refresh token via a fresh Set-Cookie header."
    )
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest httpRequest,
            @Valid @RequestBody(required = false) RefreshRequest request) {
        String refreshToken = resolveRefreshToken(httpRequest, request);
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "Refresh token is required"));
        }

        var command = new RefreshTokenCommand(refreshToken);
        Result<AuthTokens> result = mediator.send(command);

        return result.fold(
                tokens -> ResponseEntity.ok()
                        .headers(refreshCookieService.cookieHeaders(
                                refreshCookieService.issueCookieHeader(tokens.refreshToken())))
                        .body(ApiResponse.success(AuthResponse.from(
                                tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn()
                        ))),
                error -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .headers(refreshCookieService.cookieHeaders(refreshCookieService.clearCookieHeader()))
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
            description = "Revokes the active refresh token (read from the refresh_token cookie or, "
                    + "for legacy clients, the JSON body). Clears the refresh cookie. The device_token "
                    + "cookie is intentionally KEPT — same SaaS standard as Stripe/GitHub: the user's "
                    + "next login on this browser still skips the OTP step. With ?allDevices=true, "
                    + "revokes every session and trusted device for the owning user."
    )
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) RefreshRequest request,
            @RequestParam(value = "allDevices", defaultValue = "false") boolean allDevices) {

        String refreshToken = resolveRefreshToken(httpRequest, request);
        HttpHeaders clearCookie = refreshCookieService.cookieHeaders(refreshCookieService.clearCookieHeader());
        if (refreshToken == null) {
            // No token to revoke — still clear the cookie idempotently so the
            // browser stops carrying stale state.
            return ResponseEntity.ok().headers(clearCookie)
                    .body(ApiResponse.<Void>success(null));
        }

        var command = new LogoutCommand(refreshToken, allDevices);
        Result<Void> result = mediator.send(command);

        return result.fold(
                success -> ResponseEntity.ok().headers(clearCookie)
                        .body(ApiResponse.<Void>success(null)),
                error -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .headers(clearCookie)
                        .body(ApiResponse.error("UNAUTHORIZED", error))
        );
    }

    private ResponseEntity<ApiResponse<LoginResponse>> handleLogin(
            HttpServletRequest httpRequest,
            String email,
            String password,
            String requiredRole,
            String lang) {
        String deviceToken = trustedDeviceCookieService.readDeviceToken(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        var command = new LoginCommand(email, password, requiredRole, lang, deviceToken, userAgent);

        Result<LoginOutcome> result = mediator.send(command);

        return result.fold(
                outcome -> {
                    LoginResponse body = LoginResponse.from(outcome);
                    if (outcome instanceof LoginOutcome.Authenticated authenticated) {
                        // Trusted-device fast path: emit a new refresh cookie like
                        // verify-otp would. The device cookie stays in place (the
                        // backend just slid the expiry forward in DB) so we don't
                        // need to re-set it.
                        HttpHeaders headers = refreshCookieService.cookieHeaders(
                                refreshCookieService.issueCookieHeader(
                                        authenticated.tokens().refreshToken()));
                        return ResponseEntity.ok().headers(headers).body(ApiResponse.success(body));
                    }
                    return ResponseEntity.ok(ApiResponse.success(body));
                },
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

    private String resolveRefreshToken(HttpServletRequest httpRequest, RefreshRequest body) {
        String fromCookie = refreshCookieService.readRefreshToken(httpRequest);
        if (fromCookie != null) {
            return fromCookie;
        }
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return body.refreshToken();
        }
        return null;
    }

    /**
     * Original client IP — leftmost {@code X-Forwarded-For} entry when behind a
     * reverse proxy, otherwise the direct socket address. Used to stamp the
     * registration consent proof.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma == -1 ? forwarded : forwarded.substring(0, comma)).trim();
        }
        return request.getRemoteAddr();
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
