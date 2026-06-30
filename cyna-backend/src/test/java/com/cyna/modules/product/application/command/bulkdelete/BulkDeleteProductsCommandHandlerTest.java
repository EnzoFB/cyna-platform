package com.cyna.modules.product.application.command.bulkdelete;

import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BulkDeleteProductsCommandHandlerTest {

    @Mock
    private ProductRepository productRepository;

    private BulkDeleteProductsCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) { action.run(); }

        @Override
        public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new BulkDeleteProductsCommandHandler(productRepository, transactionRunner);
    }

    @Test
    void should_delete_all_provided_ids_in_bulk() {
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        var ids = List.of(id1, id2);

        Result<Void> result = handler.handle(new BulkDeleteProductsCommand(ids));

        assertThat(result.isSuccess()).isTrue();
        verify(productRepository).deleteAllByIds(ids);
    }

    @Test
    void should_fail_when_id_list_is_empty() {
        Result<Void> result = handler.handle(new BulkDeleteProductsCommand(List.of()));

        assertThat(result.isFailure()).isTrue();
        verify(productRepository, never()).deleteAllByIds(List.of());
    }

    @Test
    void should_fail_when_id_list_is_null() {
        Result<Void> result = handler.handle(new BulkDeleteProductsCommand(null));

        assertThat(result.isFailure()).isTrue();
    }
}
