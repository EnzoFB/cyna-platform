package com.cyna.modules.order.interfaces.rest;

import com.cyna.modules.order.application.command.cancel.CancelOrderCommand;
import com.cyna.modules.order.application.command.create.CreateOrderCommand;
import com.cyna.modules.order.application.query.getbyid.GetOrderByIdQuery;
import com.cyna.modules.order.application.query.getbyid.OrderReadModel;
import com.cyna.modules.order.application.query.list.ListOrdersQuery;
import com.cyna.modules.order.interfaces.rest.dto.request.CancelOrderRequest;
import com.cyna.modules.order.interfaces.rest.dto.request.CreateOrderRequest;
import com.cyna.modules.order.interfaces.rest.dto.response.OrderResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Page;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiResponse;
import com.cyna.shared.interfaces.rest.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Authenticated customer order placement and history")
public class OrderController {

    private final Mediator mediator;

    public OrderController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "Create an order",
            description = "Places a new order for the authenticated user from the supplied lines and optional billing address.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created; returns the new order id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "A business rule prevented the order from being created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createOrder(
            @AuthenticationPrincipal String userIdRaw,
            @Valid @RequestBody CreateOrderRequest request) {

        UUID userId = UUID.fromString(userIdRaw);
        var lines = request.lines().stream()
                .map(line -> new CreateOrderCommand.CreateOrderLine(
                        line.productId(),
                        line.billingCycle(),
                        line.quantity()
                ))
                .toList();

        CreateOrderCommand.BillingAddress billingAddress = null;
        if (request.billingAddress() != null) {
            var ba = request.billingAddress();
            billingAddress = new CreateOrderCommand.BillingAddress(
                    ba.line1(), ba.city(), ba.zipCode(), ba.countryCode()
            );
        }

        Result<UUID> result = mediator.send(new CreateOrderCommand(userId, lines, billingAddress));

        return result.fold(
                orderId -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(orderId)),
                error -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error))
        );
    }

    @Operation(summary = "Get an order by id",
            description = "Returns a single order owned by the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The order belongs to another user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal String userIdRaw,
            @PathVariable UUID id) {

        OrderReadModel result = mediator.send(new GetOrderByIdQuery(id));
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "Order not found: " + id));
        }

        UUID userId = UUID.fromString(userIdRaw);
        if (!result.userId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("FORBIDDEN", "Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success(OrderResponse.from(result)));
    }

    @Operation(summary = "List my orders",
            description = "Returns a paginated list of the authenticated user's orders, most recent first by default.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order page returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> listOrders(
            @AuthenticationPrincipal String userIdRaw,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        UUID userId = UUID.fromString(userIdRaw);
        Page<OrderReadModel> result = mediator.send(new ListOrdersQuery(userId, page, size, sort));

        var items = result.items().stream().map(OrderResponse::from).toList();
        var payload = PagedResponse.of(items, result.pageNumber(), result.pageSize(), result.totalElements());

        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @Operation(summary = "Cancel an order",
            description = "Cancels an order owned by the authenticated user, with a free-text reason.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The order belongs to another user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "The order cannot be cancelled in its current state")
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<UUID>> cancelOrder(
            @AuthenticationPrincipal String userIdRaw,
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request) {

        UUID userId = UUID.fromString(userIdRaw);
        Result<UUID> result = mediator.send(new CancelOrderCommand(id, userId, request.reason()));

        return result.fold(
                orderId -> ResponseEntity.ok(ApiResponse.success(orderId)),
                error -> {
                    if (error != null && error.startsWith("Order not found:")) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error("NOT_FOUND", error));
                    }
                    if ("Access denied".equals(error)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error("FORBIDDEN", error));
                    }
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error));
                }
        );
    }
}
