package com.cyna.modules.product.application.command.bulkdeletecategory;

import com.cyna.modules.product.domain.repository.CategoryRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkDeleteCategoriesCommandHandlerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private BulkDeleteCategoriesCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) { action.run(); }

        @Override
        public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new BulkDeleteCategoriesCommandHandler(categoryRepository, productRepository, transactionRunner);
    }

    @Test
    void should_delete_all_provided_categories_when_none_has_products() {
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        var ids = List.of(id1, id2);
        when(productRepository.existsByCategoryIdIn(ids)).thenReturn(false);

        Result<Void> result = handler.handle(new BulkDeleteCategoriesCommand(ids));

        assertThat(result.isSuccess()).isTrue();
        verify(categoryRepository).deleteAllByIds(ids);
    }

    @Test
    void should_fail_when_any_category_has_products() {
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        var ids = List.of(id1, id2);
        when(productRepository.existsByCategoryIdIn(ids)).thenReturn(true);

        Result<Void> result = handler.handle(new BulkDeleteCategoriesCommand(ids));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).startsWith("HAS_PRODUCTS:");
        verify(categoryRepository, never()).deleteAllByIds(ids);
    }

    @Test
    void should_fail_when_id_list_is_empty() {
        Result<Void> result = handler.handle(new BulkDeleteCategoriesCommand(List.of()));

        assertThat(result.isFailure()).isTrue();
        verify(categoryRepository, never()).deleteAllByIds(List.of());
    }
}
