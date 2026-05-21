package com.cyna.modules.product.application.command.update;

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
class UpdateProductCommandHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private UpdateProductCommandHandler handler;

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
        handler = new UpdateProductCommandHandler(productRepository, categoryRepository, transactionRunner);
    }

    @Test
    void should_update_existing_product() {
        Product existing = Product.create(
                Map.of("fr", new ProductTranslation(
                        "SOC Standard",
                        "Old service description",
                        "Old technical description",
                        List.of("Old point")
                )),
                CATEGORY_ID,
                1,
                BigDecimal.valueOf(299.99),
                BigDecimal.valueOf(2999.99),
                "EUR",
                14
        );

        UUID id = existing.getId();
        var command = new UpdateProductCommand(
                id,
                Map.of("fr", new ProductTranslation(
                        "SOC Premium",
                        "New service description",
                        "New technical description",
                        List.of("New point 1", "New point 2")
                )),
                CATEGORY_ID,
                3,
                BigDecimal.valueOf(349.99),
                BigDecimal.valueOf(3499.99),
                "EUR",
                30,
                true,
                true
        );

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(Category.reconstitute(CATEGORY_ID, "SOC",
                        Map.of("fr", new CategoryTranslation("SOC Full", "SOC desc")),
                        null, true, Instant.now(), Instant.now())));

        Result<UUID> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(id);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void should_fail_when_product_not_found() {
        UUID id = UUID.randomUUID();
        var command = new UpdateProductCommand(
                id,
                Map.of("fr", new ProductTranslation(
                        "SOC Premium",
                        "Description",
                        "Technical description",
                        List.of()
                )),
                CATEGORY_ID,
                3,
                BigDecimal.valueOf(349.99),
                BigDecimal.valueOf(3499.99),
                "EUR",
                0,
                true,
                true
        );

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        Result<UUID> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Product not found: " + id);
        verify(productRepository, never()).save(any(Product.class));
    }
}
