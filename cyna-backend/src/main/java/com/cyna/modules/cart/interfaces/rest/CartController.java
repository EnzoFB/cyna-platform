package com.cyna.modules.cart.interfaces.rest;

import com.cyna.modules.cart.application.command.addline.AddCartLineCommand;
import com.cyna.modules.cart.application.command.checkout.CheckoutCartCommand;
import com.cyna.modules.cart.application.command.mergeguest.MergeGuestCartCommand;
import com.cyna.modules.cart.application.command.removeline.RemoveCartLineCommand;
import com.cyna.modules.cart.application.command.updatebillingcycle.UpdateCartLineBillingCycleCommand;
import com.cyna.modules.cart.application.command.updatequantity.UpdateCartLineQuantityCommand;
import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.modules.cart.application.model.CheckoutCartReadModel;
import com.cyna.modules.cart.application.query.getcart.GetCartQuery;
import com.cyna.modules.cart.interfaces.dto.request.AddCartLineRequest;
import com.cyna.modules.cart.interfaces.dto.request.MergeGuestCartRequest;
import com.cyna.modules.cart.interfaces.dto.request.UpdateCartLineBillingCycleRequest;
import com.cyna.modules.cart.interfaces.dto.request.UpdateCartLineQuantityRequest;
import com.cyna.modules.cart.interfaces.dto.response.CartResponse;
import com.cyna.modules.cart.interfaces.dto.response.CheckoutCartResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final Mediator mediator;

    public CartController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken) {
        Result<OwnerContext> ownerResult = resolveOwner(userId, guestToken, true);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }

        OwnerContext owner = ownerResult.getValue();
        CartReadModel result = mediator.send(new GetCartQuery(owner.userId(), owner.guestToken()));
        return ResponseEntity.ok(ApiResponse.success(CartResponse.from(result)));
    }

    @PostMapping("/lines")
    public ResponseEntity<ApiResponse<CartResponse>> addLine(
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Valid @RequestBody AddCartLineRequest request) {
        Result<OwnerContext> ownerResult = resolveOwner(userId, guestToken, true);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }
        OwnerContext owner = ownerResult.getValue();

        var command = new AddCartLineCommand(
                owner.userId(),
                owner.guestToken(),
                request.productId(),
                request.billingCycle(),
                request.quantity()
        );

        Result<CartReadModel> result = mediator.send(command);
        return mapCartResult(result);
    }

    @PatchMapping("/lines/{lineId}/quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateLineQuantity(
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @PathVariable UUID lineId,
            @Valid @RequestBody UpdateCartLineQuantityRequest request) {
        Result<OwnerContext> ownerResult = resolveOwner(userId, guestToken, true);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }
        OwnerContext owner = ownerResult.getValue();

        Result<CartReadModel> result = mediator.send(new UpdateCartLineQuantityCommand(
                owner.userId(),
                owner.guestToken(),
                lineId,
                request.quantity()
        ));
        return mapCartResult(result);
    }

    @PatchMapping("/lines/{lineId}/billing-cycle")
    public ResponseEntity<ApiResponse<CartResponse>> updateLineBillingCycle(
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @PathVariable UUID lineId,
            @Valid @RequestBody UpdateCartLineBillingCycleRequest request) {
        Result<OwnerContext> ownerResult = resolveOwner(userId, guestToken, true);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }
        OwnerContext owner = ownerResult.getValue();

        Result<CartReadModel> result = mediator.send(new UpdateCartLineBillingCycleCommand(
                owner.userId(),
                owner.guestToken(),
                lineId,
                request.billingCycle()
        ));
        return mapCartResult(result);
    }

    @DeleteMapping("/lines/{lineId}")
    public ResponseEntity<ApiResponse<CartResponse>> deleteLine(
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @PathVariable UUID lineId) {
        Result<OwnerContext> ownerResult = resolveOwner(userId, guestToken, true);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }
        OwnerContext owner = ownerResult.getValue();

        Result<CartReadModel> result = mediator.send(new RemoveCartLineCommand(
                owner.userId(),
                owner.guestToken(),
                lineId
        ));
        return mapCartResult(result);
    }

    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<CartResponse>> mergeGuestCart(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody MergeGuestCartRequest request) {
        Result<OwnerContext> ownerResult = resolveOwner(userId, null, false);
        if (ownerResult.isFailure()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", ownerResult.getError()));
        }

        Result<CartReadModel> result = mediator.send(new MergeGuestCartCommand(
                ownerResult.getValue().userId(),
                request.guestToken()
        ));
        return mapCartResult(result);
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutCartResponse>> checkout(
            @AuthenticationPrincipal String userId) {
        Result<OwnerContext> ownerResult = resolveOwner(userId, null, false);
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
        if (error.startsWith("Exactly one owner")
                || error.startsWith("Guest token is required")
                || error.startsWith("Authenticated user is required")
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
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error));
    }

    private Result<OwnerContext> resolveOwner(String userIdRaw, String guestToken, boolean allowGuest) {
        if (userIdRaw != null && !userIdRaw.isBlank()) {
            try {
                return Result.success(new OwnerContext(UUID.fromString(userIdRaw), null));
            } catch (IllegalArgumentException ex) {
                return Result.failure("Invalid user id in authentication principal");
            }
        }

        if (!allowGuest) {
            return Result.failure("Authenticated user is required");
        }

        if (guestToken == null || guestToken.isBlank()) {
            return Result.failure("Guest token is required for anonymous cart operations");
        }

        return Result.success(new OwnerContext(null, guestToken.trim()));
    }

    private record OwnerContext(UUID userId, String guestToken) {
    }
}
