package com.cyna.modules.product.application.command.deletecategory;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.CategoryTranslation;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteCategoryCommandHandlerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private DeleteCategoryCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) { action.run(); }

        @Override
        public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new DeleteCategoryCommandHandler(categoryRepository, productRepository, transactionRunner);
    }

    @Test
    void should_delete_category_when_it_has_no_products() {
        var id = UUID.randomUUID();
        var existing = Category.reconstitute(id, "Antivirus",
                Map.of("fr", new CategoryTranslation("Antivirus", "desc")),
                null, true, Instant.now(), Instant.now());
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.countByCategoryId(id)).thenReturn(0L);

        Result<Void> result = handler.handle(new DeleteCategoryCommand(id));

        assertThat(result.isSuccess()).isTrue();
        verify(categoryRepository).deleteById(id);
    }

    @Test
    void should_fail_when_category_not_found() {
        var id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        Result<Void> result = handler.handle(new DeleteCategoryCommand(id));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Category not found");
        verify(categoryRepository, never()).deleteById(id);
    }

    @Test
    void should_fail_when_category_still_has_products() {
        var id = UUID.randomUUID();
        var category = Category.reconstitute(id, "Antivirus",
                Map.of("fr", new CategoryTranslation("Antivirus", "desc")),
                null, true, Instant.now(), Instant.now());
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryId(id)).thenReturn(3L);

        Result<Void> result = handler.handle(new DeleteCategoryCommand(id));

        assertThat(result.isFailure()).isTrue();
        // The handler surfaces a structured error string the UI parses to show
        // "this category contains N product(s) and can't be deleted" — assert on
        // both the prefix (so the UI's discriminator still works) and on the count.
        assertThat(result.getError()).startsWith("HAS_PRODUCTS:");
        assertThat(result.getError()).contains("3 produit");
        verify(categoryRepository, never()).deleteById(id);
    }
}
