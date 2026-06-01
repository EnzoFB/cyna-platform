package com.cyna.modules.product.application.command.create;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.CategoryTranslation;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.ProductTranslation;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductCommandHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private CreateProductCommandHandler handler;

    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) {
            action.run();
        }

        @Override
        public <T> T runReturning(Supplier<T> action) {
            return action.get();
        }
    };

    @BeforeEach
    void setUp() {
        handler = new CreateProductCommandHandler(productRepository, categoryRepository, transactionRunner);
    }

    @Test
    void should_create_product_successfully() {
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(Category.reconstitute(CATEGORY_ID, "SOC",
                        Map.of("fr", new CategoryTranslation("SOC Full", "SOC desc")),
                        null, true, Instant.now(), Instant.now())));

        var command = new CreateProductCommand(
                Map.of("fr", new ProductTranslation(
                        "SOC Standard",
                        "Managed SOC service",
                        "24/7 monitoring",
                        List.of("24/7 monitoring")
                )),
                CATEGORY_ID,
                2,
                BigDecimal.valueOf(299.99),
                BigDecimal.valueOf(2999.99),
                "EUR",
                14
        );

        Result<UUID> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void should_fail_when_category_not_found() {
        UUID unknownCategoryId = UUID.randomUUID();
        when(categoryRepository.findById(unknownCategoryId)).thenReturn(Optional.empty());

        var command = new CreateProductCommand(
                Map.of("fr", new ProductTranslation(
                        "SOC Standard",
                        "Managed SOC service",
                        "24/7 monitoring",
                        List.of()
                )),
                unknownCategoryId,
                1,
                BigDecimal.valueOf(299.99),
                BigDecimal.valueOf(2999.99),
                "EUR",
                0
        );

        Result<UUID> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Category not found");
        verify(productRepository, never()).save(any(Product.class));
    }
}
