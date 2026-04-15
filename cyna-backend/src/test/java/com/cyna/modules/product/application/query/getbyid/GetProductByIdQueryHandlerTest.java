package com.cyna.modules.product.application.query.getbyid;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProductByIdQueryHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private GetProductByIdQueryHandler handler;

    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new GetProductByIdQueryHandler(productRepository, categoryRepository);
    }

    @Test
    void should_return_product_read_model() {
        Product product = Product.create(
                "EDR Pro",
                CATEGORY_ID,
                2,
                "Endpoint detection service",
                "Behavioral analysis",
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                30,
                List.of("Threat detection")
        );

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(Category.reconstitute(CATEGORY_ID, "EDR", "EDR Full", "EDR desc", null, true, Instant.now(), Instant.now())));

        ProductReadModel result = handler.handle(new GetProductByIdQuery(product.getId()));

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(product.getId());
        assertThat(result.name()).isEqualTo("EDR Pro");
        assertThat(result.categoryId()).isEqualTo(CATEGORY_ID);
        assertThat(result.categoryName()).isEqualTo("EDR");
        assertThat(result.priorityLevel()).isEqualTo(2);
        assertThat(result.freeTrialDays()).isEqualTo(30);
        assertThat(result.highlightPoints()).containsExactly("Threat detection");
    }

    @Test
    void should_return_null_when_not_found() {
        var id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        ProductReadModel result = handler.handle(new GetProductByIdQuery(id));

        assertThat(result).isNull();
    }
}
