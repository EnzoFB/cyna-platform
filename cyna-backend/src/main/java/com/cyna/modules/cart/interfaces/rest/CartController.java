package com.cyna.modules.cart.interfaces.rest;

import com.cyna.modules.cart.application.command.addline.AddCartLineCommand;
import com.cyna.modules.cart.application.command.checkout.CheckoutCartCommand;
import com.cyna.modules.cart.application.command.removeline.RemoveCartLineCommand;
import com.cyna.modules.cart.application.command.updatebillingcycle.UpdateCartLineBillingCycleCommand;
import com.cyna.modules.cart.application.command.updatequantity.UpdateCartLineQuantityCommand;
import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.modules.cart.application.model.CheckoutCartReadModel;
import com.cyna.modules.cart.application.query.getcart.GetCartQuery;
import com.cyna.modules.cart.interfaces.dto.request.AddCartLineRequest;
import com.cyna.modules.cart.interfaces.dto.request.UpdateCartLineBillingCycleRequest;
import com.cyna.modules.cart.interfaces.dto.request.UpdateCartLineQuantityRequest;
import com.cyna.modules.cart.interfaces.dto.response.CartResponse;
import com.cyna.modules.cart.interfaces.dto.response.CheckoutCartResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Authenticated user's server-side shopping cart")
public class CartController {

    private final Mediator mediator;

    public CartController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "Get my cart",
            description = "Returns the authenticated user's active cart, creating an empty one if none exists.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Authenticated user is required or invalid")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal String userId) {
        Result<OwnerContext> ownerResult = resolveOwner(userId);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }

        OwnerContext owner = ownerResult.getValue();
        CartReadModel result = mediator.send(new GetCartQuery(owner.userId()));
        return ResponseEntity.ok(ApiResponse.success(CartResponse.from(result)));
    }

    @Operation(summary = "Add a cart line",
            description = "Adds a product to the cart, or increments its quantity if the same product and billing cycle is already present.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Authenticated user is required or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "A business rule prevented the change")
    })
    @PostMapping("/lines")
    public ResponseEntity<ApiResponse<CartResponse>> addLine(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody AddCartLineRequest request) {
        Result<OwnerContext> ownerResult = resolveOwner(userId);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }
        OwnerContext owner = ownerResult.getValue();

        var command = new AddCartLineCommand(
                owner.userId(),
                request.productId(),
                request.billingCycle(),
                request.quantity()
        );

        Result<CartReadModel> result = mediator.send(command);
        return mapCartResult(result);
    }

    @Operation(summary = "Update cart line quantity",
            description = "Sets the quantity of an existing cart line.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Authenticated user is required or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart or cart line not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "A business rule prevented the change")
    })
    @PatchMapping("/lines/{lineId}/quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateLineQuantity(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID lineId,
            @Valid @RequestBody UpdateCartLineQuantityRequest request) {
        Result<OwnerContext> ownerResult = resolveOwner(userId);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }
        OwnerContext owner = ownerResult.getValue();

        Result<CartReadModel> result = mediator.send(new UpdateCartLineQuantityCommand(
                owner.userId(),
                lineId,
                request.quantity()
        ));
        return mapCartResult(result);
    }

    @Operation(summary = "Update cart line billing cycle",
            description = "Switches an existing cart line between monthly and annual billing.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Authenticated user is required or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart or cart line not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "A business rule prevented the change")
    })
    @PatchMapping("/lines/{lineId}/billing-cycle")
    public ResponseEntity<ApiResponse<CartResponse>> updateLineBillingCycle(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID lineId,
            @Valid @RequestBody UpdateCartLineBillingCycleRequest request) {
        Result<OwnerContext> ownerResult = resolveOwner(userId);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }
        OwnerContext owner = ownerResult.getValue();

        Result<CartReadModel> result = mediator.send(new UpdateCartLineBillingCycleCommand(
                owner.userId(),
                lineId,
                request.billingCycle()
        ));
        return mapCartResult(result);
    }

    @Operation(summary = "Remove a cart line",
            description = "Removes a line from the cart.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Authenticated user is required or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart or cart line not found")
    })
    @DeleteMapping("/lines/{lineId}")
    public ResponseEntity<ApiResponse<CartResponse>> deleteLine(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID lineId) {
        Result<OwnerContext> ownerResult = resolveOwner(userId);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }
        OwnerContext owner = ownerResult.getValue();

        Result<CartReadModel> result = mediator.send(new RemoveCartLineCommand(
                owner.userId(),
                lineId
        ));
        return mapCartResult(result);
    }

    @Operation(summary = "Merge guest cart (deprecated)", deprecated = true,
            description = "Deprecated since 2026-04-01: guest carts are no longer stored server-side. Always returns 410 Gone.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Endpoint removed; guest carts live only in the client cache"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Authenticated user is required or invalid")
    })
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<CartResponse>> mergeGuestCart(
            @AuthenticationPrincipal String userId) {
        Result<OwnerContext> ownerResult = resolveOwner(userId);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }
        String message = "Guest cart merge is deprecated: guest carts are no longer stored server-side. "
                + "Since 2026-04-01, guest carts live only in the client cache (10-day TTL). "
                + "Please sign in to persist carts on the server.";
        return ResponseEntity.status(HttpStatus.GONE).body(ApiResponse.error("GONE", message));
    }

    @Operation(summary = "Check out the cart",
            description = "Converts the active cart into an order ready for payment.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Checkout succeeded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Authenticated user is required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Active cart not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cart is already checked out"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "A business rule prevented checkout")
    })
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutCartResponse>> checkout(
            @AuthenticationPrincipal String userId) {
        Result<OwnerContext> ownerResult = resolveOwner(userId);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }

        Result<CheckoutCartReadModel> result = mediator.send(new CheckoutCartCommand(ownerResult.getValue().userId()));
        return result.fold(
                success -> ResponseEntity.ok(ApiResponse.success(CheckoutCartResponse.from(success))),
                error -> mapCheckoutError(error)
        );
    }

    private ResponseEntity<ApiResponse<CartResponse>> mapCartResult(Result<CartReadModel> result) {
        return result.fold(
                success -> ResponseEntity.ok(ApiResponse.success(CartResponse.from(success))),
                error -> mapCartError(error)
        );
    }

    private ResponseEntity<ApiResponse<CartResponse>> mapCartError(String error) {
        if (error.startsWith("Product not found:") || error.startsWith("Cart line not found:")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", error));
        }
        if (error.startsWith("Active cart not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", error));
        }
        if (error.startsWith("Authenticated user is required")
                || error.startsWith("Invalid user id")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", error));
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error));
    }

    private ResponseEntity<ApiResponse<CheckoutCartResponse>> mapCheckoutError(String error) {
        if (error.startsWith("Active cart not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", error));
        }
        if (error.startsWith("Authenticated user is required")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", error));
        }
        if (error.startsWith("Cart is already checked out")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("CONFLICT", error));
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error));
    }

    private Result<OwnerContext> resolveOwner(String userIdRaw) {
        if (userIdRaw != null && !userIdRaw.isBlank()) {
            try {
                return Result.success(new OwnerContext(UUID.fromString(userIdRaw)));
            } catch (IllegalArgumentException ex) {
                return Result.failure("Invalid user id in authentication principal");
            }
        }

        return Result.failure("Authenticated user is required");
    }

    private record OwnerContext(UUID userId) {
    }
}
