package com.cyna.modules.product.interfaces.rest;

import com.cyna.modules.product.application.query.getcategorybyid.CategoryReadModel;
import com.cyna.modules.product.application.query.getcategorybyid.GetCategoryByIdQuery;
import com.cyna.modules.product.application.translation.CategoryTranslationDto;
import com.cyna.modules.product.application.query.listcategories.ListCategoriesQuery;
import com.cyna.modules.product.interfaces.dto.response.CategoryResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryControllerCachingTest {

    @Mock
    private Mediator mediator;

    private CategoryController controller;

    @BeforeEach
    void setUp() {
        controller = new CategoryController(mediator);
    }

    @Test
    void should_return_cache_headers_when_listing_categories() {
        when(mediator.send(any(ListCategoriesQuery.class))).thenReturn(List.of(sampleCategory()));

        var request = new MockHttpServletRequest("GET", "/api/v1/categories");
        var response = new MockHttpServletResponse();
        ResponseEntity<ApiResponse<List<CategoryResponse>>> result =
                controller.listCategories(null, new ServletWebRequest(request, response));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getHeaders().getCacheControl()).contains("max-age=3600").contains("public");
        assertThat(result.getHeaders().getETag()).isNotBlank();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().success()).isTrue();
    }

    @Test
    void should_return_not_modified_when_getting_category_with_matching_etag() {
        when(mediator.send(any(GetCategoryByIdQuery.class))).thenReturn(sampleCategory());
        UUID categoryId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        var firstRequest = new MockHttpServletRequest("GET", "/api/v1/categories/" + categoryId);
        var firstResponse = new MockHttpServletResponse();
        ResponseEntity<ApiResponse<CategoryResponse>> firstCall =
                controller.getCategoryById(categoryId, new ServletWebRequest(firstRequest, firstResponse));

        String etag = firstCall.getHeaders().getETag();

        var secondRequest = new MockHttpServletRequest("GET", "/api/v1/categories/" + categoryId);
        secondRequest.addHeader("If-None-Match", etag);
        var secondResponse = new MockHttpServletResponse();
        ResponseEntity<ApiResponse<CategoryResponse>> secondCall =
                controller.getCategoryById(categoryId, new ServletWebRequest(secondRequest, secondResponse));

        assertThat(secondCall.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
        assertThat(secondCall.getBody()).isNull();
        assertThat(secondCall.getHeaders().getCacheControl()).contains("max-age=3600").contains("public");
        assertThat(secondCall.getHeaders().getETag()).isEqualTo(etag);
    }

    private CategoryReadModel sampleCategory() {
        return new CategoryReadModel(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "xdr",
                Map.of("fr", new CategoryTranslationDto("Extended detection and response", "XDR category description")),
                "demo-image".getBytes(),
                true,
                0L,
                Instant.parse("2026-04-01T10:00:00Z"),
                Instant.parse("2026-04-27T10:00:00Z")
        );
    }
}
