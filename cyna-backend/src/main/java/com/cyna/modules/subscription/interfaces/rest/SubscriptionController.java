package com.cyna.modules.subscription.interfaces.rest;

import com.cyna.modules.subscription.application.command.autorenew.UpdateSubscriptionAutoRenewCommand;
import com.cyna.modules.subscription.application.command.cancel.CancelSubscriptionCommand;
import com.cyna.modules.subscription.application.query.getbyid.GetSubscriptionByIdQuery;
import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.modules.subscription.application.query.list.ListSubscriptionsQuery;
import com.cyna.modules.subscription.application.query.list.SubscriptionSort;
import com.cyna.modules.subscription.interfaces.rest.dto.request.UpdateSubscriptionAutoRenewRequest;
import com.cyna.modules.subscription.interfaces.rest.dto.response.SubscriptionResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final Mediator mediator;

    public SubscriptionController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscriptionById(
            @AuthenticationPrincipal String userIdRaw,
            @PathVariable UUID id) {

        SubscriptionReadModel result = mediator.send(new GetSubscriptionByIdQuery(id));
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "Subscription not found: " + id));
        }

        UUID userId = UUID.fromString(userIdRaw);
        if (!result.userId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("FORBIDDEN", "Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success(SubscriptionResponse.from(result)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SubscriptionResponse>>> listSubscriptions(
            @AuthenticationPrincipal String userIdRaw,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        var sortResult = SubscriptionSort.parse(sort);
        if (sortResult.isFailure()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_SORT", sortResult.getError()));
        }

        UUID userId = UUID.fromString(userIdRaw);
        var query = new ListSubscriptionsQuery(userId, page, size, sortResult.getValue());
        Page<SubscriptionReadModel> result = mediator.send(query);

        var items = result.items().stream().map(SubscriptionResponse::from).toList();
        var payload = PagedResponse.of(items, result.pageNumber(), result.pageSize(), result.totalElements());

        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancelSubscription(
            @AuthenticationPrincipal String userIdRaw,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(userIdRaw);
        Result<SubscriptionReadModel> result = mediator.send(new CancelSubscriptionCommand(id, userId));

        return result.fold(
                model -> ResponseEntity.ok(ApiResponse.success(SubscriptionResponse.from(model))),
                error -> {
                    if (error != null && error.startsWith("Subscription not found:")) {
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

    @PutMapping("/{id}/auto-renew")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> updateAutoRenew(
            @AuthenticationPrincipal String userIdRaw,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubscriptionAutoRenewRequest request) {

        UUID userId = UUID.fromString(userIdRaw);
        Result<SubscriptionReadModel> result = mediator.send(
                new UpdateSubscriptionAutoRenewCommand(id, userId, Boolean.TRUE.equals(request.autoRenew()))
        );

        return result.fold(
                model -> ResponseEntity.ok(ApiResponse.success(SubscriptionResponse.from(model))),
                error -> {
                    if (error != null && error.startsWith("Subscription not found:")) {
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
