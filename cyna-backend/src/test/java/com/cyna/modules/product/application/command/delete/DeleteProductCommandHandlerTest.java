package com.cyna.modules.product.application.command.delete;

import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteProductCommandHandlerTest {

    @Mock
    private ProductRepository productRepository;

    private DeleteProductCommandHandler handler;

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
        handler = new DeleteProductCommandHandler(productRepository, transactionRunner);
    }

    @Test
    void should_delete_existing_product() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(true);

        Result<Void> result = handler.handle(new DeleteProductCommand(id));

        assertThat(result.isSuccess()).isTrue();
        verify(productRepository).deleteById(id);
    }

    @Test
    void should_fail_when_product_not_found() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(false);

        Result<Void> result = handler.handle(new DeleteProductCommand(id));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Product not found: " + id);
        verify(productRepository, never()).deleteById(id);
    }
}
