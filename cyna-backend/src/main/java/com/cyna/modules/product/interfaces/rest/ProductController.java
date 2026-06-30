package com.cyna.modules.product.interfaces.rest;

import com.cyna.modules.product.application.query.getbyid.GetProductByIdQuery;
import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.application.query.list.ListProductsQuery;
import com.cyna.modules.product.interfaces.dto.response.ProductDetailResponse;
import com.cyna.modules.product.interfaces.dto.response.ProductResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Page;
import com.cyna.shared.interfaces.rest.ApiCachePolicies;
import com.cyna.shared.interfaces.rest.ApiResponse;
import com.cyna.shared.interfaces.rest.EtagGenerator;
import com.cyna.shared.interfaces.rest.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Public, read-only product catalog. Responses are cacheable.
 * Admin write operations and uncached admin reads live in {@link AdminProductController}.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Public read-only product catalog")
public class ProductController {

    private final Mediator mediator;

    public ProductController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "List products",
            description = "Returns a paginated, filterable and sortable list of catalog products. Public and cacheable "
                    + "(supports ETag / 304). All filters are optional and combine with AND semantics.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product page returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "304", description = "Not modified (ETag matched)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid price range or negative filter value")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> listProducts(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by published flag")
            @RequestParam(required = false) Boolean published,
            @Parameter(description = "Filter by availability flag")
            @RequestParam(required = false) Boolean available,
            @Parameter(description = "Filter by a single category id")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Filter by several category ids (OR within the set)")
            @RequestParam(required = false) List<UUID> categoryIds,
            @Parameter(description = "Full-text search on name/description")
            @RequestParam(required = false) String search,
            @Parameter(description = "Minimum monthly price (inclusive)")
            @RequestParam(required = false) BigDecimal monthlyPriceMin,
            @Parameter(description = "Maximum monthly price (inclusive)")
            @RequestParam(required = false) BigDecimal monthlyPriceMax,
            @Parameter(description = "Minimum annual price (inclusive)")
            @RequestParam(required = false) BigDecimal annualPriceMin,
            @Parameter(description = "Maximum annual price (inclusive)")
            @RequestParam(required = false) BigDecimal annualPriceMax,
            @Parameter(description = "Only products offering at least this many free-trial days")
            @RequestParam(required = false) Integer minFreeTrialDays,
            @Parameter(description = "Sort as `field,dir` (e.g. priority,desc | monthlyPrice,asc | name,asc)", example = "priority,desc")
            @RequestParam(defaultValue = "priority,desc") String sort,
            @Parameter(description = "When true, only return products whose category is active")
            @RequestParam(required = false) Boolean activeCategoryOnly,
            WebRequest webRequest) {

        if (isInvalidRange(monthlyPriceMin, monthlyPriceMax)
                || isInvalidRange(annualPriceMin, annualPriceMax)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            "INVALID_PRICE_RANGE",
                            "Minimum price cannot be greater than maximum price"
                    ));
        }

        if (isNegative(monthlyPriceMin) || isNegative(monthlyPriceMax)
                || isNegative(annualPriceMin) || isNegative(annualPriceMax)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            "INVALID_PRICE_FILTER",
                            "Price filters must be greater than or equal to zero"
                    ));
        }

        if (minFreeTrialDays != null && minFreeTrialDays < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            "INVALID_FREE_TRIAL_DAYS",
                            "minFreeTrialDays must be greater than or equal to zero"
                    ));
        }

        List<UUID> normalizedCategoryIds = ProductQuerySupport.normalizeCategoryIds(categoryIds);

        var query = new ListProductsQuery(
                page,
                size,
                published,
                available,
                categoryId,
                normalizedCategoryIds,
                search,
                monthlyPriceMin,
                monthlyPriceMax,
                annualPriceMin,
                annualPriceMax,
                minFreeTrialDays,
                sort,
                activeCategoryOnly
        );
        Page<ProductReadModel> result = mediator.send(query);

        var items = result.items().stream().map(ProductResponse::from).toList();
        var payload = PagedResponse.of(items, result.pageNumber(), result.pageSize(), result.totalElements());
        String etag = EtagGenerator.from(
                page,
                size,
                published,
                available,
                categoryId,
                normalizedCategoryIds,
                search,
                monthlyPriceMin,
                monthlyPriceMax,
                annualPriceMin,
                annualPriceMax,
                minFreeTrialDays,
                sort,
                activeCategoryOnly,
                payload
        );

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
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(
            @Parameter(description = "Product id") @PathVariable UUID id, WebRequest webRequest) {
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

    private boolean isInvalidRange(BigDecimal min, BigDecimal max) {
        return min != null && max != null && min.compareTo(max) > 0;
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }
}
