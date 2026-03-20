package com.cyna.modules.product.application.command.update;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.ProductCategory;
import com.cyna.modules.product.domain.model.ProductPriority;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    private UpdateProductCommandHandler handler;

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
        handler = new UpdateProductCommandHandler(productRepository, transactionRunner);
    }

    @Test
    void should_update_existing_product() {
        Product existing = Product.create(
                "SOC Standard",
                ProductCategory.SOC,
                ProductPriority.NORMALE,
                "Old service description",
                "Old technical description",
                Money.of(299.99, "EUR"),
                Money.of(2999.99, "EUR")
        );

        UUID id = existing.getId();
        var command = new UpdateProductCommand(
                id,
                "SOC Premium",
                ProductCategory.SOC,
                ProductPriority.HAUTE,
                "New service description",
                "New technical description",
                BigDecimal.valueOf(349.99),
                BigDecimal.valueOf(3499.99),
                "EUR"
        );

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));

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
                "SOC Premium",
                ProductCategory.SOC,
                ProductPriority.HAUTE,
                "Description",
                "Technical description",
                BigDecimal.valueOf(349.99),
                BigDecimal.valueOf(3499.99),
                "EUR"
        );

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        Result<UUID> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Product not found: " + id);
        verify(productRepository, never()).save(any(Product.class));
    }
}
