package com.cyna.modules.product.application.command.deletecategory;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.repository.CategoryRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteCategoryCommandHandlerTest {

    @Mock
    private CategoryRepository categoryRepository;

    private DeleteCategoryCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) { action.run(); }

        @Override
        public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new DeleteCategoryCommandHandler(categoryRepository, transactionRunner);
    }

    @Test
    void should_deactivate_category_successfully() {
        var id = UUID.randomUUID();
        var existing = Category.reconstitute(id, "Antivirus", "desc", null, true, Instant.now(), Instant.now());
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));

        Result<Void> result = handler.handle(new DeleteCategoryCommand(id));

        assertThat(result.isSuccess()).isTrue();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void should_fail_when_category_not_found() {
        var id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        Result<Void> result = handler.handle(new DeleteCategoryCommand(id));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Category not found");
    }

    @Test
    void should_fail_when_category_already_inactive() {
        var id = UUID.randomUUID();
        var inactive = Category.reconstitute(id, "Antivirus", "desc", null, false, Instant.now(), Instant.now());
        when(categoryRepository.findById(id)).thenReturn(Optional.of(inactive));

        Result<Void> result = handler.handle(new DeleteCategoryCommand(id));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Category is already inactive");
    }
}
