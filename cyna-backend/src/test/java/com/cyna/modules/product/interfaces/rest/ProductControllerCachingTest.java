package com.cyna.modules.product.interfaces.rest;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.application.query.list.ListProductsQuery;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Page;
import com.cyna.shared.interfaces.rest.ApiResponse;
import com.cyna.shared.interfaces.rest.PagedResponse;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerCachingTest {

    @Mock
    private Mediator mediator;

    private ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(mediator);
    }

    @Test
    void should_return_cache_headers_when_listing_products() {
        var page = new Page<>(List.of(sampleProduct()), 0, 20, 1, 1);
        when(mediator.send(any(ListProductsQuery.class))).thenReturn(page);

        var request = new MockHttpServletRequest("GET", "/api/v1/products");
        var response = new MockHttpServletResponse();

        ResponseEntity<ApiResponse<PagedResponse<com.cyna.modules.product.interfaces.dto.response.ProductResponse>>> result =
                controller.listProducts(0, 20, true, null, null, null, null,
                        null, null, null, null, null, "priority,desc", new ServletWebRequest(request, response));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getHeaders().getCacheControl()).contains("max-age=300").contains("public");
        assertThat(result.getHeaders().getETag()).isNotBlank();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().success()).isTrue();
    }

    @Test
    void should_return_not_modified_when_listing_products_with_matching_etag() {
        var page = new Page<>(List.of(sampleProduct()), 0, 20, 1, 1);
        when(mediator.send(any(ListProductsQuery.class))).thenReturn(page);

        var firstRequest = new MockHttpServletRequest("GET", "/api/v1/products");
        var firstResponse = new MockHttpServletResponse();
        ResponseEntity<ApiResponse<PagedResponse<com.cyna.modules.product.interfaces.dto.response.ProductResponse>>> firstCall =
                controller.listProducts(0, 20, true, null, null, null, null,
                        null, null, null, null, null, "priority,desc",
                        new ServletWebRequest(firstRequest, firstResponse));

        String etag = firstCall.getHeaders().getETag();

        var secondRequest = new MockHttpServletRequest("GET", "/api/v1/products");
        secondRequest.addHeader("If-None-Match", etag);
        var secondResponse = new MockHttpServletResponse();
        ResponseEntity<ApiResponse<PagedResponse<com.cyna.modules.product.interfaces.dto.response.ProductResponse>>> secondCall =
                controller.listProducts(0, 20, true, null, null, null, null,
                        null, null, null, null, null, "priority,desc",
                        new ServletWebRequest(secondRequest, secondResponse));

        assertThat(secondCall.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
        assertThat(secondCall.getBody()).isNull();
        assertThat(secondCall.getHeaders().getCacheControl()).contains("max-age=300").contains("public");
        assertThat(secondCall.getHeaders().getETag()).isEqualTo(etag);
    }

    private ProductReadModel sampleProduct() {
        return new ProductReadModel(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "XDR Ultimate",
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "XDR",
                1,
                "Managed XDR service",
                "Technical details",
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                true,
                true,
                14,
                List.of("24/7 SOC"),
                List.of("https://cdn.cyna.com/images/xdr-1.png"),
                Instant.parse("2026-04-20T10:00:00Z"),
                Instant.parse("2026-04-27T10:00:00Z")
        );
    }
}
