package com.cyna.modules.product.interfaces.rest;

import com.cyna.modules.product.application.command.createcategory.CreateCategoryCommand;
import com.cyna.modules.product.application.command.deletecategory.DeleteCategoryCommand;
import com.cyna.modules.product.application.command.updatecategory.UpdateCategoryCommand;
import com.cyna.modules.product.application.command.updatecategoryimage.UpdateCategoryImageCommand;
import com.cyna.modules.product.application.query.getcategorybyid.CategoryReadModel;
import com.cyna.modules.product.application.query.getcategorybyid.GetCategoryByIdQuery;
import com.cyna.modules.product.application.query.listcategories.ListCategoriesQuery;
import com.cyna.modules.product.interfaces.dto.request.CreateCategoryRequest;
import com.cyna.modules.product.interfaces.dto.request.UpdateCategoryRequest;
import com.cyna.modules.product.interfaces.dto.response.CategoryResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiCachePolicies;
import com.cyna.shared.interfaces.rest.ApiResponse;
import com.cyna.shared.interfaces.rest.EtagGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "CRUD operations for product categories")
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
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories(WebRequest webRequest) {
        List<CategoryReadModel> categories = mediator.send(new ListCategoriesQuery());
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
        CategoryReadModel result = mediator.send(new GetCategoryByIdQuery(id));

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

    @Operation(summary = "Create category", description = "Creates a new category. Requires ADMIN role.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business rule violation (e.g. name already taken)")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        var command = new CreateCategoryCommand(
                request.name(),
                request.fullName(),
                request.description() != null ? request.description() : ""
        );

        Result<UUID> result = mediator.send(command);

        return result.fold(
                id -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(id)),
                error -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error))
        );
    }

    @Operation(summary = "Update category", description = "Updates an existing category. Requires ADMIN role.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UUID>> updateCategory(@PathVariable UUID id,
                                                             @Valid @RequestBody UpdateCategoryRequest request) {
        var command = new UpdateCategoryCommand(
                id,
                request.name(),
                request.fullName(),
                request.description() != null ? request.description() : "",
                request.active()
        );

        Result<UUID> result = mediator.send(command);

        return result.fold(
                categoryId -> ResponseEntity.ok(ApiResponse.success(categoryId)),
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

    @Operation(summary = "Upload category image", description = "Uploads or replaces the image for a category. Requires ADMIN role.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Image uploaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PatchMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadImage(@PathVariable UUID id,
                                             @RequestPart MultipartFile image) throws IOException {
        byte[] imageBytes = image.getBytes();
        Result<Void> result = mediator.send(new UpdateCategoryImageCommand(id, imageBytes));

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

    @Operation(summary = "Delete category", description = "Hard-deletes a category. Blocked if the category has linked products. Requires ADMIN role.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Category deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Category has linked products")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable UUID id) {
        Result<Void> result = mediator.send(new DeleteCategoryCommand(id));

        return result.fold(
                ignored -> ResponseEntity.noContent().build(),
                error -> {
                    if (isNotFoundError(error)) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                    }
                    if (error != null && error.startsWith("HAS_PRODUCTS:")) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error("HAS_PRODUCTS", error.substring("HAS_PRODUCTS:".length())));
                    }
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
                }
        );
    }

    private boolean isNotFoundError(String error) {
        return error != null && error.startsWith("Category not found:");
    }
}
