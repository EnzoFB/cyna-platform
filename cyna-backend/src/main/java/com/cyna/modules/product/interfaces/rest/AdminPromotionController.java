package com.cyna.modules.product.interfaces.rest;

import com.cyna.modules.product.application.command.createpromotion.CreatePromotionCommand;
import com.cyna.modules.product.application.command.deletepromotion.DeletePromotionCommand;
import com.cyna.modules.product.application.command.updatepromotion.UpdatePromotionCommand;
import com.cyna.modules.product.application.command.updateoffercarouselsettings.UpdateOfferCarouselSettingsCommand;
import com.cyna.modules.product.application.query.getoffercarouselsettings.GetOfferCarouselSettingsQuery;
import com.cyna.modules.product.application.query.getpromotionbyid.GetPromotionByIdQuery;
import com.cyna.modules.product.application.query.listpromotions.ListPromotionsQuery;
import com.cyna.modules.product.application.query.listpromotions.PromotionReadModel;
import com.cyna.modules.product.interfaces.dto.request.CreatePromotionRequest;
import com.cyna.modules.product.interfaces.dto.request.UpdateOfferCarouselSettingsRequest;
import com.cyna.modules.product.interfaces.dto.request.UpdatePromotionRequest;
import com.cyna.modules.product.interfaces.dto.response.OfferCarouselSettingsResponse;
import com.cyna.modules.product.interfaces.dto.response.PromotionResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/promotions")
@Tag(name = "Admin Promotions", description = "Manage product promotions from back office")
public class AdminPromotionController {

    private final Mediator mediator;

    public AdminPromotionController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "List promotions")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Promotion list returned")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> list() {
        List<PromotionReadModel> promotions = mediator.send(new ListPromotionsQuery());
        return ResponseEntity.ok(ApiResponse.success(promotions.stream().map(PromotionResponse::from).toList()));
    }

    @Operation(summary = "Get promotion by id")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Promotion found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Promotion not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getById(@PathVariable UUID id) {
        PromotionReadModel promotion = mediator.send(new GetPromotionByIdQuery(id));
        if (promotion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "Promotion not found: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success(PromotionResponse.from(promotion)));
    }

    @Operation(summary = "Get offers carousel fixed text settings")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Settings returned")
    })
    @GetMapping("/carousel-settings")
    public ResponseEntity<ApiResponse<OfferCarouselSettingsResponse>> getCarouselSettings() {
        var settings = mediator.send(new GetOfferCarouselSettingsQuery());
        return ResponseEntity.ok(ApiResponse.success(OfferCarouselSettingsResponse.from(settings)));
    }

    @Operation(summary = "Create promotion")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Promotion created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Promotion overlap"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> create(@Valid @RequestBody CreatePromotionRequest request) {
        Result<UUID> result = mediator.send(new CreatePromotionCommand(
                request.productId(),
                request.discountPercent(),
                request.translations(),
                request.startAt(),
                request.endAt(),
                request.enabled(),
                request.showInCarousel(),
                request.carouselOrder()
        ));
        return mapIdResult(result, true);
    }

    @Operation(summary = "Update promotion")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Promotion updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Promotion not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Promotion overlap"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UUID>> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdatePromotionRequest request) {
        Result<UUID> result = mediator.send(new UpdatePromotionCommand(
                id,
                request.discountPercent(),
                request.translations(),
                request.startAt(),
                request.endAt(),
                request.enabled(),
                request.showInCarousel(),
                request.carouselOrder()
        ));
        return mapIdResult(result, false);
    }

    @Operation(summary = "Delete promotion")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Promotion deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Promotion not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Result<Void> result = mediator.send(new DeletePromotionCommand(id));
        return result.fold(
                ignored -> ResponseEntity.noContent().build(),
                error -> {
                    if (error != null && error.startsWith("NOT_FOUND:")) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                    }
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
                }
        );
    }

    @Operation(summary = "Update offers carousel fixed text settings")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Settings updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PutMapping("/carousel-settings")
    public ResponseEntity<Void> updateCarouselSettings(@Valid @RequestBody UpdateOfferCarouselSettingsRequest request) {
        Result<Void> result = mediator.send(new UpdateOfferCarouselSettingsCommand(
                request.translations()
        ));
        return result.fold(
                ignored -> ResponseEntity.noContent().build(),
                error -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build()
        );
    }

    private ResponseEntity<ApiResponse<UUID>> mapIdResult(Result<UUID> result, boolean created) {
        return result.fold(
                id -> created
                        ? ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(id))
                        : ResponseEntity.ok(ApiResponse.success(id)),
                error -> {
                    if (error == null) {
                        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ApiResponse.error("VALIDATION_ERROR", "Unknown promotion error"));
                    }
                    if (error.startsWith("PRODUCT_NOT_FOUND:") || error.startsWith("NOT_FOUND:")) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error("NOT_FOUND", error.substring(error.indexOf(':') + 1)));
                    }
                    if (error.startsWith("PROMOTION_OVERLAP:")) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error("PROMOTION_OVERLAP", error.substring(error.indexOf(':') + 1)));
                    }
                    if (error.startsWith("CAROUSEL_ORDER_CONFLICT:")) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error("CAROUSEL_ORDER_CONFLICT", error.substring(error.indexOf(':') + 1)));
                    }
                    if (error.startsWith("CAROUSEL_ORDER_OUT_OF_RANGE:")) {
                        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ApiResponse.error("CAROUSEL_ORDER_OUT_OF_RANGE", error.substring(error.indexOf(':') + 1)));
                    }
                    if (error.startsWith("VALIDATION_ERROR:")) {
                        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ApiResponse.error("VALIDATION_ERROR", error.substring(error.indexOf(':') + 1)));
                    }
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error));
                }
        );
    }
}
