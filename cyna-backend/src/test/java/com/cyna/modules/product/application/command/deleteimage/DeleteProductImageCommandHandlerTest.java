package com.cyna.modules.product.application.command.deleteimage;

import com.cyna.modules.product.domain.model.ProductImage;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteProductImageCommandHandlerTest {

    @Mock
    private ProductImageRepository productImageRepository;

    private DeleteProductImageCommandHandler handler;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID IMAGE_ID = UUID.randomUUID();

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

    private ProductImage imageAtOrder(int order) {
        return ProductImage.reconstitute(
                IMAGE_ID, PRODUCT_ID, new byte[]{1}, "image/png",
                order, Instant.now(), Instant.now()
        );
    }

    @BeforeEach
    void setUp() {
        handler = new DeleteProductImageCommandHandler(productImageRepository, transactionRunner);
    }

    @Test
    void should_delete_image_and_decrement_order_of_subsequent_images() {
        ProductImage image = imageAtOrder(2);
        when(productImageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));

        var command = new DeleteProductImageCommand(PRODUCT_ID, IMAGE_ID);
        Result<Void> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        verify(productImageRepository).deleteById(IMAGE_ID);
        verify(productImageRepository).decrementDisplayOrderAfter(PRODUCT_ID, 2);
    }

    @Test
    void should_fail_when_image_not_found() {
        when(productImageRepository.findById(IMAGE_ID)).thenReturn(Optional.empty());

        var command = new DeleteProductImageCommand(PRODUCT_ID, IMAGE_ID);
        Result<Void> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Image not found");
        verify(productImageRepository, never()).deleteById(IMAGE_ID);
    }

    @Test
    void should_fail_when_image_belongs_to_a_different_product() {
        UUID otherProductId = UUID.randomUUID();
        ProductImage imageOfOtherProduct = ProductImage.reconstitute(
                IMAGE_ID, otherProductId, new byte[]{1}, "image/png",
                0, Instant.now(), Instant.now()
        );
        when(productImageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(imageOfOtherProduct));

        var command = new DeleteProductImageCommand(PRODUCT_ID, IMAGE_ID);
        Result<Void> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("does not belong to product");
        verify(productImageRepository, never()).deleteById(IMAGE_ID);
    }
}
