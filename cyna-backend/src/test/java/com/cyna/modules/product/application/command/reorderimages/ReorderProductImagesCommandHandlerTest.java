package com.cyna.modules.product.application.command.reorderimages;

import com.cyna.modules.product.domain.model.ProductImage;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReorderProductImagesCommandHandlerTest {

    @Mock
    private ProductImageRepository productImageRepository;

    private ReorderProductImagesCommandHandler handler;

    private static final UUID PRODUCT_ID = UUID.randomUUID();

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

    private ProductImage image(UUID id, int order) {
        return ProductImage.reconstitute(
                id, PRODUCT_ID, new byte[]{1}, "image/png",
                order, Instant.now(), Instant.now()
        );
    }

    @BeforeEach
    void setUp() {
        handler = new ReorderProductImagesCommandHandler(productImageRepository, transactionRunner);
    }

    @Test
    void should_apply_two_pass_update_to_avoid_unique_constraint_conflicts() {
        UUID id0 = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(productImageRepository.findByProductId(PRODUCT_ID))
                .thenReturn(List.of(image(id0, 0), image(id1, 1), image(id2, 2)));

        // Reorder: move id2 to front
        var command = new ReorderProductImagesCommand(PRODUCT_ID, List.of(id2, id0, id1));
        Result<Void> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();

        // Pass 1: temporary values n+i to avoid UNIQUE conflicts during the swap
        // Pass 2: final values i
        InOrder order = inOrder(productImageRepository);
        order.verify(productImageRepository).updateDisplayOrder(id2, 3);
        order.verify(productImageRepository).updateDisplayOrder(id0, 4);
        order.verify(productImageRepository).updateDisplayOrder(id1, 5);
        order.verify(productImageRepository).updateDisplayOrder(id2, 0);
        order.verify(productImageRepository).updateDisplayOrder(id0, 1);
        order.verify(productImageRepository).updateDisplayOrder(id1, 2);
    }

    @Test
    void should_fail_when_ordered_list_contains_unknown_image_id() {
        UUID knownId = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();

        when(productImageRepository.findByProductId(PRODUCT_ID))
                .thenReturn(List.of(image(knownId, 0)));

        var command = new ReorderProductImagesCommand(PRODUCT_ID, List.of(knownId, unknownId));
        Result<Void> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Image not found for this product");
        verify(productImageRepository, never()).updateDisplayOrder(knownId, 0);
    }

    @Test
    void should_fail_when_ordered_list_does_not_cover_all_images() {
        UUID id0 = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();

        when(productImageRepository.findByProductId(PRODUCT_ID))
                .thenReturn(List.of(image(id0, 0), image(id1, 1)));

        // Only one ID provided instead of two
        var command = new ReorderProductImagesCommand(PRODUCT_ID, List.of(id0));
        Result<Void> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("orderedImageIds must contain all image IDs");
        verify(productImageRepository, never()).updateDisplayOrder(id0, 0);
    }
}
