package com.cyna.modules.product.application.query.list;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.ProductCategory;
import com.cyna.modules.product.domain.model.ProductPriority;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListProductsQueryHandlerTest {

    @Mock
    private ProductRepository productRepository;

    private ListProductsQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ListProductsQueryHandler(productRepository);
    }

    @Test
    void should_return_paged_products() {
        Product product = Product.create(
                "XDR Ultimate",
                ProductCategory.XDR,
                ProductPriority.HAUTE,
                "XDR service",
                "Cross-domain telemetry",
                Money.of(399.99, "EUR"),
                Money.of(3999.99, "EUR")
        );

        Page<Product> page = new Page<>(
                List.of(product),
                0,
                20,
                1,
                1
        );

        ProductSort sort = ProductSort.parse("createdAt,desc").getValue();

        when(productRepository.findAll(0, 20, "PUBLISHED", "XDR", "xdr", sort))
                .thenReturn(page);

        Page<ProductReadModel> result = handler.handle(
                new ListProductsQuery(0, 20, "PUBLISHED", "XDR", "xdr", sort)
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().name()).isEqualTo("XDR Ultimate");
        assertThat(result.items().getFirst().priority()).isEqualTo("HAUTE");
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void should_sanitize_pagination_values() {
        Page<Product> emptyPage = new Page<>(List.of(), 0, 100, 0, 0);

        ProductSort sort = ProductSort.parse("createdAt,desc").getValue();

        when(productRepository.findAll(0, 100, null, null, null, sort))
                .thenReturn(emptyPage);

        Page<ProductReadModel> result = handler.handle(
                new ListProductsQuery(-2, 999, null, null, null, sort)
        );

        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(100);
    }
}
