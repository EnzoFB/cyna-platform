package com.cyna.modules.order.interfaces.rest;

import com.cyna.modules.order.application.command.cancel.CancelOrderCommand;
import com.cyna.modules.order.application.query.admin.AdminOrderReadModel;
import com.cyna.modules.order.application.query.admin.ListAllOrdersQuery;
import com.cyna.modules.order.application.query.getbyid.GetOrderByIdQuery;
import com.cyna.modules.order.application.query.getbyid.OrderReadModel;
import com.cyna.modules.order.interfaces.rest.dto.request.CancelOrderRequest;
import com.cyna.modules.order.interfaces.rest.dto.response.AdminOrderListResponse;
import com.cyna.modules.order.interfaces.rest.dto.response.OrderResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Page;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiResponse;
import com.cyna.shared.interfaces.rest.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final Mediator mediator;

    public AdminOrderController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AdminOrderListResponse>>> listAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        Page<AdminOrderReadModel> result = mediator.send(new ListAllOrdersQuery(status, safePage, safeSize));

        var items = result.items().stream().map(AdminOrderListResponse::from).toList();
        var payload = PagedResponse.of(items, result.pageNumber(), result.pageSize(), result.totalElements());

        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        OrderReadModel result = mediator.send(new GetOrderByIdQuery(id));
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "Order not found: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.from(result)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<UUID>> cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request) {

        // Fetch the order first to obtain its owner's userId.
        // The CancelOrderCommandHandler validates userId ownership, so we pass the
        // actual owner's id — admins can cancel any order without bypassing that logic.
        OrderReadModel order = mediator.send(new GetOrderByIdQuery(id));
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "Order not found: " + id));
        }

        Result<UUID> result = mediator.send(new CancelOrderCommand(id, order.userId(), request.reason()));

        return result.fold(
                orderId -> ResponseEntity.ok(ApiResponse.success(orderId)),
                error -> {
                    if (error != null && error.startsWith("Order not found:")) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error("NOT_FOUND", error));
                    }
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error));
                }
        );
    }
}
