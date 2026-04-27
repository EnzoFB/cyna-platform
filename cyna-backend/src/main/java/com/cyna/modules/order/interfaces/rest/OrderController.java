package com.cyna.modules.order.interfaces.rest;

import com.cyna.modules.order.application.command.cancel.CancelOrderCommand;
import com.cyna.modules.order.application.command.create.CreateOrderCommand;
import com.cyna.modules.order.application.query.getbyid.GetOrderByIdQuery;
import com.cyna.modules.order.application.query.getbyid.OrderReadModel;
import com.cyna.modules.order.application.query.list.ListOrdersQuery;
import com.cyna.modules.order.application.query.list.OrderSort;
import com.cyna.modules.order.interfaces.rest.dto.request.CancelOrderRequest;
import com.cyna.modules.order.interfaces.rest.dto.request.CreateOrderRequest;
import com.cyna.modules.order.interfaces.rest.dto.response.OrderResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Page;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiResponse;
import com.cyna.shared.interfaces.rest.PagedResponse;
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
public class OrderController {

    private final Mediator mediator;

    public OrderController(Mediator mediator) {
        this.mediator = mediator;
    }

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

        Result<UUID> result = mediator.send(new CreateOrderCommand(userId, lines));

        return result.fold(
                orderId -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(orderId)),
                error -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error))
        );
    }

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

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> listOrders(
            @AuthenticationPrincipal String userIdRaw,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        var sortResult = OrderSort.parse(sort);
        if (sortResult.isFailure()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_SORT", sortResult.getError()));
        }

        UUID userId = UUID.fromString(userIdRaw);
        Page<OrderReadModel> result = mediator.send(new ListOrdersQuery(userId, page, size, sortResult.getValue()));

        var items = result.items().stream().map(OrderResponse::from).toList();
        var payload = PagedResponse.of(items, result.pageNumber(), result.pageSize(), result.totalElements());

        return ResponseEntity.ok(ApiResponse.success(payload));
    }

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
