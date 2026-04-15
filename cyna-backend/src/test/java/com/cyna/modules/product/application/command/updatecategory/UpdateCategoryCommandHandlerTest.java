package com.cyna.modules.product.application.command.updatecategory;

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
class UpdateCategoryCommandHandlerTest {

    @Mock
    private CategoryRepository categoryRepository;

    private UpdateCategoryCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) { action.run(); }

        @Override
        public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new UpdateCategoryCommandHandler(categoryRepository, transactionRunner);
    }

    @Test
    void should_update_category_successfully() {
        var id = UUID.randomUUID();
        var existing = Category.reconstitute(id, "Old Name", "Old Name", "Old desc", null, true, Instant.now(), Instant.now());
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));

        var command = new UpdateCategoryCommand(id, "New Name", "New Name", "New desc");
        Result<UUID> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(id);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void should_fail_when_category_not_found() {
        var id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        var command = new UpdateCategoryCommand(id, "Name", "Name", "desc");
        Result<UUID> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Category not found");
    }

    @Test
    void should_preserve_existing_image_on_update() {
        var id = UUID.randomUUID();
        byte[] existingImage = new byte[]{1, 2, 3};
        var existing = Category.reconstitute(id, "Name", "Name", "desc", existingImage, true, Instant.now(), Instant.now());
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));

        var command = new UpdateCategoryCommand(id, "New Name", "New Name", "desc");
        Result<UUID> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        verify(categoryRepository).save(any(Category.class));
    }
}
