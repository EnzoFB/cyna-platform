package com.cyna.modules.product.application.query.list;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.domain.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListProductsQueryHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    private ListProductsQueryHandler handler;

    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListProductsQueryHandler(productRepository, categoryRepository, productImageRepository);
    }

    @Test
    void should_return_paged_products() {
        Product product = Product.create(
                "XDR Ultimate",
                CATEGORY_ID,
                3,
                "XDR service",
                "Cross-domain telemetry",
                BigDecimal.valueOf(399.99),
                BigDecimal.valueOf(3999.99),
                "EUR",
                30,
                List.of("Extended protection")
        );

        Page<Product> page = new Page<>(
                List.of(product),
                0,
                20,
                1,
                1
        );

        ProductSort sort = ProductSort.parse("createdAt,desc").getValue();

        when(categoryRepository.findAll()).thenReturn(
                List.of(Category.reconstitute(CATEGORY_ID, "XDR", "XDR Full", "XDR desc", null, true, Instant.now(), Instant.now()))
        );
        when(productImageRepository.findByProductIds(List.of(product.getId()))).thenReturn(List.of());
        when(productRepository.findAll(0, 20, true, CATEGORY_ID, "xdr", sort))
                .thenReturn(page);

        Page<ProductReadModel> result = handler.handle(
                new ListProductsQuery(0, 20, true, CATEGORY_ID, "xdr", sort)
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().name()).isEqualTo("XDR Ultimate");
        assertThat(result.items().getFirst().categoryName()).isEqualTo("XDR");
        assertThat(result.items().getFirst().priorityLevel()).isEqualTo(3);
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void should_sanitize_pagination_values() {
        Page<Product> emptyPage = new Page<>(List.of(), 0, 100, 0, 0);

        ProductSort sort = ProductSort.parse("createdAt,desc").getValue();

        when(categoryRepository.findAll()).thenReturn(List.of());
        when(productImageRepository.findByProductIds(List.of())).thenReturn(List.of());
        when(productRepository.findAll(0, 100, null, null, null, sort))
                .thenReturn(emptyPage);

        Page<ProductReadModel> result = handler.handle(
                new ListProductsQuery(-2, 999, null, null, null, sort)
        );

        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(100);
    }
}
