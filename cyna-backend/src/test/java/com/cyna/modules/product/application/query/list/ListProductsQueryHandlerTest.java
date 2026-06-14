package com.cyna.modules.product.application.query.list;

import com.cyna.modules.product.domain.repository.ProductSort;
import com.cyna.modules.product.application.promotion.PromotionPricingResolver;
import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.CategoryTranslation;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.ProductTranslation;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.domain.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListProductsQueryHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private PromotionRepository promotionRepository;

    private final PromotionPricingResolver promotionPricingResolver = new PromotionPricingResolver();

    private ListProductsQueryHandler handler;

    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListProductsQueryHandler(
                productRepository,
                categoryRepository,
                productImageRepository,
                promotionRepository,
                promotionPricingResolver
        );
        when(promotionRepository.findActiveByProductIds(anyCollection(), any(Instant.class))).thenReturn(List.of());
    }

    @Test
    void should_return_paged_products() {
        Product product = Product.create(
                Map.of("fr", new ProductTranslation(
                        "XDR Ultimate",
                        "XDR service",
                        "Cross-domain telemetry",
                        List.of("Extended protection")
                )),
                CATEGORY_ID,
                3,
                BigDecimal.valueOf(399.99),
                BigDecimal.valueOf(3999.99),
                "EUR",
                30
        );

        Page<Product> page = new Page<>(
                List.of(product),
                0,
                20,
                1,
                1
        );

        String sort = "createdAt,desc";

        when(categoryRepository.findAll()).thenReturn(
                List.of(Category.reconstitute(CATEGORY_ID, "XDR",
                        Map.of("fr", new CategoryTranslation("XDR Full", "XDR desc")),
                        null, true, Instant.now(), Instant.now()))
        );
        when(productImageRepository.findByProductIds(List.of(product.getId()))).thenReturn(List.of());
        when(productRepository.findAll(0, 20, true, null, CATEGORY_ID, null, "xdr",
                null, null, null, null, null, ProductSort.parseOrDefault(sort)))
                .thenReturn(page);

        Page<ProductReadModel> result = handler.handle(
                new ListProductsQuery(0, 20, true, null, CATEGORY_ID, null, "xdr",
                        null, null, null, null, null, sort)
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().translations().get("fr").name()).isEqualTo("XDR Ultimate");
        assertThat(result.items().getFirst().categoryName()).isEqualTo("XDR");
        assertThat(result.items().getFirst().priorityLevel()).isEqualTo(3);
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void should_include_images_as_base64_in_read_model() {
        Product product = Product.create(
                Map.of("fr", new ProductTranslation(
                        "XDR Ultimate",
                        "XDR service",
                        "Cross-domain telemetry",
                        List.of()
                )),
                CATEGORY_ID,
                3,
                BigDecimal.valueOf(399.99),
                BigDecimal.valueOf(3999.99),
                "EUR",
                30
        );

        byte[] imageBytes = new byte[]{10, 20, 30};
        UUID imageId = UUID.randomUUID();
        var productImage = com.cyna.modules.product.domain.model.ProductImage.reconstitute(
                imageId, product.getId(), imageBytes, "image/jpeg", 0, Instant.now(), Instant.now()
        );

        Page<Product> page = new Page<>(List.of(product), 0, 20, 1, 1);
        String sort = "createdAt,desc";

        when(categoryRepository.findAll()).thenReturn(
                List.of(Category.reconstitute(CATEGORY_ID, "XDR",
                        Map.of("fr", new CategoryTranslation("XDR Full", "XDR desc")),
                        null, true, Instant.now(), Instant.now()))
        );
        when(productImageRepository.findByProductIds(List.of(product.getId()))).thenReturn(List.of(productImage));
        when(productRepository.findAll(0, 20, true, null, null, null, null,
                null, null, null, null, null, ProductSort.parseOrDefault(sort)))
                .thenReturn(page);

        Page<ProductReadModel> result = handler.handle(
                new ListProductsQuery(0, 20, true, null, null, null, null,
                        null, null, null, null, null, sort)
        );

        assertThat(result.items()).hasSize(1);
        var images = result.items().getFirst().images();
        assertThat(images).hasSize(1);
        assertThat(images.getFirst().id()).isEqualTo(imageId);
        assertThat(images.getFirst().base64()).isEqualTo(Base64.getEncoder().encodeToString(imageBytes));
    }

    @Test
    void should_sanitize_pagination_values() {
        Page<Product> emptyPage = new Page<>(List.of(), 0, 100, 0, 0);

        String sort = "createdAt,desc";

        when(categoryRepository.findAll()).thenReturn(List.of());
        when(productImageRepository.findByProductIds(List.of())).thenReturn(List.of());
        when(productRepository.findAll(0, 100, null, null, null, null, null,
                null, null, null, null, null, ProductSort.parseOrDefault(sort)))
                .thenReturn(emptyPage);

        Page<ProductReadModel> result = handler.handle(
                new ListProductsQuery(-2, 999, null, null, null, null, null,
                        null, null, null, null, null, sort)
        );

        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(100);
    }
}
