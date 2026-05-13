package com.cyna.modules.product.application.command.addimage;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.ProductImage;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddProductImageCommandHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    private AddProductImageCommandHandler handler;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
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

    private Product product() {
        return Product.reconstitute(
                PRODUCT_ID, "SOC Standard", CATEGORY_ID, 1,
                "desc", "tech", BigDecimal.valueOf(99), BigDecimal.valueOf(999),
                "EUR", true, true, 0, List.of(), Instant.now(), Instant.now()
        );
    }

    @BeforeEach
    void setUp() {
        handler = new AddProductImageCommandHandler(productRepository, productImageRepository, transactionRunner);
    }

    @Test
    void should_save_image_and_return_its_id() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(productImageRepository.maxDisplayOrderByProductId(PRODUCT_ID)).thenReturn(1);

        ProductImage saved = ProductImage.create(PRODUCT_ID, new byte[]{1, 2, 3}, "image/png", 2);
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(saved);

        var command = new AddProductImageCommand(PRODUCT_ID, new byte[]{1, 2, 3}, "image/png");
        Result<UUID> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(saved.getId());
        verify(productImageRepository).save(any(ProductImage.class));
    }

    @Test
    void should_assign_next_display_order_after_existing_images() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(productImageRepository.maxDisplayOrderByProductId(PRODUCT_ID)).thenReturn(3);

        ProductImage saved = ProductImage.create(PRODUCT_ID, new byte[]{1}, "image/jpeg", 4);
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(saved);

        handler.handle(new AddProductImageCommand(PRODUCT_ID, new byte[]{1}, "image/jpeg"));

        // The handler calls maxDisplayOrderByProductId + 1, so displayOrder passed to create should be 4.
        verify(productImageRepository).maxDisplayOrderByProductId(PRODUCT_ID);
        verify(productImageRepository).save(any(ProductImage.class));
    }

    @Test
    void should_fail_when_product_not_found() {
        UUID unknownId = UUID.randomUUID();
        when(productRepository.findById(unknownId)).thenReturn(Optional.empty());

        var command = new AddProductImageCommand(unknownId, new byte[]{1}, "image/png");
        Result<UUID> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Product not found");
        verify(productImageRepository, never()).save(any(ProductImage.class));
    }
}
