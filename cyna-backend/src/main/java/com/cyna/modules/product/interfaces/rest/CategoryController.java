package com.cyna.modules.product.interfaces.rest;

import com.cyna.modules.product.application.query.getcategorybyid.CategoryReadModel;
import com.cyna.modules.product.application.query.getcategorybyid.GetCategoryByIdQuery;
import com.cyna.modules.product.application.query.listcategories.ListCategoriesQuery;
import com.cyna.modules.product.interfaces.dto.response.CategoryResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiCachePolicies;
import com.cyna.shared.interfaces.rest.ApiResponse;
import com.cyna.shared.interfaces.rest.EtagGenerator;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.UUID;

/**
 * Public, read-only category catalog. Responses are cacheable.
 * Admin write operations and uncached admin reads live in {@link AdminCategoryController}.
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Public read-only product categories")
public class CategoryController {

    private final Mediator mediator;

    public CategoryController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "List all categories", description = "Returns all categories. Public access.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category list returned")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories(
            @RequestParam(required = false) Boolean activeOnly,
            WebRequest webRequest) {
        List<CategoryReadModel> categories = mediator.send(new ListCategoriesQuery(activeOnly));
        List<CategoryResponse> response = categories.stream().map(CategoryResponse::from).toList();
        String etag = EtagGenerator.from(response);

        if (webRequest.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(ApiCachePolicies.STATIC_CONFIGURATION)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity.ok()
                .cacheControl(ApiCachePolicies.STATIC_CONFIGURATION)
                .eTag(etag)
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "Get category by id", description = "Returns a category by UUID. Public access.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable UUID id, WebRequest webRequest) {
        CategoryReadModel result = mediator.send(new GetCategoryByIdQuery(id, true));

        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "Category not found: " + id));
        }

        CategoryResponse response = CategoryResponse.from(result);
        String etag = EtagGenerator.from(response);

        if (webRequest.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(ApiCachePolicies.STATIC_CONFIGURATION)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity.ok()
                .cacheControl(ApiCachePolicies.STATIC_CONFIGURATION)
                .eTag(etag)
                .body(ApiResponse.success(response));
    }
}
