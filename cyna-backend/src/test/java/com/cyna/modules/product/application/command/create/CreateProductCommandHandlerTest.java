package com.cyna.modules.product.application.command.create;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.ProductCategory;
import com.cyna.modules.product.domain.model.ProductPriority;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateProductCommandHandlerTest {

    @Mock
    private ProductRepository productRepository;

    private CreateProductCommandHandler handler;

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
        handler = new CreateProductCommandHandler(productRepository, transactionRunner);
    }

    @Test
    void should_create_product_successfully() {
        var command = new CreateProductCommand(
                "SOC Standard",
                ProductCategory.SOC,
                ProductPriority.MOYENNE,
                "Managed SOC service",
                "24/7 monitoring",
                BigDecimal.valueOf(299.99),
                BigDecimal.valueOf(2999.99),
                "EUR"
        );

        Result<UUID> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isNotNull();
        verify(productRepository).save(any(Product.class));
    }
}
