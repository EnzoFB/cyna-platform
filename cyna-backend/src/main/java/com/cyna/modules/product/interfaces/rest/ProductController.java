package com.cyna.modules.product.interfaces.rest;

import com.cyna.modules.product.application.command.create.CreateProductCommand;
import com.cyna.modules.product.application.command.delete.DeleteProductCommand;
import com.cyna.modules.product.application.command.update.UpdateProductCommand;
import com.cyna.modules.product.application.query.getbyid.GetProductByIdQuery;
import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.application.query.list.ListProductsQuery;
import com.cyna.modules.product.application.query.list.ProductSort;
import com.cyna.modules.product.interfaces.dto.request.CreateProductRequest;
import com.cyna.modules.product.interfaces.dto.request.UpdateProductRequest;
import com.cyna.modules.product.interfaces.dto.response.ProductDetailResponse;
import com.cyna.modules.product.interfaces.dto.response.ProductResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Page;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiCachePolicies;
import com.cyna.shared.interfaces.rest.ApiResponse;
import com.cyna.shared.interfaces.rest.EtagGenerator;
import com.cyna.shared.interfaces.rest.PagedResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "CRUD operations for product catalog")
public class ProductController {

    private final Mediator mediator;

    public ProductController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "List products", description = "Returns a paginated list of products")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product list returned")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "priority,desc") String sort,
            WebRequest webRequest) {

        var sortResult = ProductSort.parse(sort);
        if (sortResult.isFailure()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_SORT", sortResult.getError()));
        }

        var query = new ListProductsQuery(page, size, published, available, categoryId, search, sortResult.getValue());
        Page<ProductReadModel> result = mediator.send(query);

        var items = result.items().stream().map(ProductResponse::from).toList();
        var payload = PagedResponse.of(items, result.pageNumber(), result.pageSize(), result.totalElements());
        String etag = EtagGenerator.from(page, size, published, categoryId, search, sort, payload);

        if (webRequest.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(ApiCachePolicies.PRODUCT_CATALOG)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity.ok()
                .cacheControl(ApiCachePolicies.PRODUCT_CATALOG)
                .eTag(etag)
                .body(ApiResponse.success(payload));
    }

    @Operation(summary = "Get product by id", description = "Returns a product by UUID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable UUID id, WebRequest webRequest) {
        var query = new GetProductByIdQuery(id);
        ProductReadModel result = mediator.send(query);

        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "Product not found: " + id));
        }

        ProductDetailResponse response = ProductDetailResponse.from(result);
        String etag = EtagGenerator.from(response);

        if (webRequest.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(ApiCachePolicies.PRODUCT_CATALOG)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity.ok()
                .cacheControl(ApiCachePolicies.PRODUCT_CATALOG)
                .eTag(etag)
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "Create product", description = "Creates a product in DRAFT status")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        var command = new CreateProductCommand(
                request.name(),
                request.categoryId(),
                request.priorityLevel(),
                request.serviceDescription(),
                request.technicalDescription(),
                request.monthlyPrice(),
                request.annualPrice(),
                request.currency(),
                request.freeTrialDays(),
                request.highlightPoints()
        );

        Result<UUID> result = mediator.send(command);

        return result.fold(
                productId -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(productId)),
                error -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error))
        );
    }

    @Operation(summary = "Update product", description = "Fully updates a product")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UUID>> updateProduct(@PathVariable UUID id,
                                                           @Valid @RequestBody UpdateProductRequest request) {
        var command = new UpdateProductCommand(
                id,
                request.name(),
                request.categoryId(),
                request.priorityLevel(),
                request.serviceDescription(),
                request.technicalDescription(),
                request.monthlyPrice(),
                request.annualPrice(),
                request.currency(),
                request.freeTrialDays(),
                request.highlightPoints()
        );

        Result<UUID> result = mediator.send(command);

        return result.fold(
                productId -> ResponseEntity.ok(ApiResponse.success(productId)),
                error -> {
                    if (isNotFoundError(error)) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error("NOT_FOUND", error));
                    }
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error));
                }
        );
    }

    @Operation(summary = "Delete product", description = "Deletes a product by UUID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Product deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        var command = new DeleteProductCommand(id);
        Result<Void> result = mediator.send(command);

        return result.fold(
                ignored -> ResponseEntity.noContent().build(),
                error -> {
                    if (isNotFoundError(error)) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                    }
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
                }
        );
    }

    private boolean isNotFoundError(String error) {
        return error != null && error.startsWith("Product not found:");
    }
}
