package com.cyna.modules.product.application.query.getbyid;

import com.cyna.modules.product.application.promotion.PromotionPricingResolver;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.CategoryTranslation;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.ProductTranslation;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProductByIdQueryHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private PromotionRepository promotionRepository;

    private final PromotionPricingResolver promotionPricingResolver = new PromotionPricingResolver();

    private GetProductByIdQueryHandler handler;

    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new GetProductByIdQueryHandler(
                productRepository,
                categoryRepository,
                productImageRepository,
                promotionRepository,
                promotionPricingResolver
        );
    }

    @Test
    void should_return_product_read_model() {
        Product product = Product.create(
                Map.of("fr", new ProductTranslation(
                        "EDR Pro",
                        "Endpoint detection service",
                        "Behavioral analysis",
                        List.of("Threat detection")
                )),
                CATEGORY_ID,
                2,
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                30
        );

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(Category.reconstitute(CATEGORY_ID, "EDR",
                        Map.of("fr", new CategoryTranslation("EDR Full", "EDR desc")),
                        null, true, Instant.now(), Instant.now())));
        when(productImageRepository.findByProductId(product.getId())).thenReturn(List.of());
        when(promotionRepository.findActiveByProductIds(anyCollection(), any(Instant.class))).thenReturn(List.of());

        ProductReadModel result = handler.handle(new GetProductByIdQuery(product.getId()));

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(product.getId());
        assertThat(result.translations().get("fr").name()).isEqualTo("EDR Pro");
        assertThat(result.categoryId()).isEqualTo(CATEGORY_ID);
        assertThat(result.categoryName()).isEqualTo("EDR");
        assertThat(result.priorityLevel()).isEqualTo(2);
        assertThat(result.freeTrialDays()).isEqualTo(30);
        assertThat(result.translations().get("fr").highlightPoints()).containsExactly("Threat detection");
    }

    @Test
    void should_include_images_as_base64_in_read_model() {
        Product product = Product.create(
                Map.of("fr", new ProductTranslation(
                        "EDR Pro",
                        "Endpoint detection service",
                        "Behavioral analysis",
                        List.of()
                )),
                CATEGORY_ID,
                2,
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                30
        );

        byte[] imageBytes = new byte[]{1, 2, 3, 4};
        UUID imageId = UUID.randomUUID();
        var productImage = com.cyna.modules.product.domain.model.ProductImage.reconstitute(
                imageId, product.getId(), imageBytes, "image/png", 0, Instant.now(), Instant.now()
        );

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(Category.reconstitute(CATEGORY_ID, "EDR",
                        Map.of("fr", new CategoryTranslation("EDR Full", "EDR desc")),
                        null, true, Instant.now(), Instant.now())));
        when(productImageRepository.findByProductId(product.getId())).thenReturn(List.of(productImage));
        when(promotionRepository.findActiveByProductIds(anyCollection(), any(Instant.class))).thenReturn(List.of());

        ProductReadModel result = handler.handle(new GetProductByIdQuery(product.getId()));

        assertThat(result.images()).hasSize(1);
        assertThat(result.images().getFirst().id()).isEqualTo(imageId);
        assertThat(result.images().getFirst().base64()).isEqualTo(Base64.getEncoder().encodeToString(imageBytes));
    }

    @Test
    void should_return_null_when_not_found() {
        var id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        ProductReadModel result = handler.handle(new GetProductByIdQuery(id));

        assertThat(result).isNull();
    }

    @Test
    void should_return_null_when_category_is_inactive() {
        Product product = Product.create(
                Map.of("fr", new ProductTranslation(
                        "EDR Pro",
                        "Endpoint detection service",
                        "Behavioral analysis",
                        List.of()
                )),
                CATEGORY_ID,
                2,
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                0
        );

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(Category.reconstitute(CATEGORY_ID, "EDR",
                        Map.of("fr", new CategoryTranslation("EDR Full", "EDR desc")),
                        null, false, Instant.now(), Instant.now())));

        ProductReadModel result = handler.handle(new GetProductByIdQuery(product.getId()));

        assertThat(result).isNull();
    }
}
