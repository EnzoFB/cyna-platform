package com.cyna.modules.product.application.command.createcategory;

import com.cyna.modules.product.application.translation.CategoryTranslationDto;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCategoryCommandHandlerTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CreateCategoryCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) { action.run(); }

        @Override
        public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new CreateCategoryCommandHandler(categoryRepository, transactionRunner);
    }

    @Test
    void should_create_category_successfully() {
        when(categoryRepository.existsByName("Antivirus")).thenReturn(false);

        var command = new CreateCategoryCommand("Antivirus",
                Map.of("fr", new CategoryTranslationDto("Antivirus", "Logiciels antivirus")));
        Result<UUID> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isNotNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void should_fail_when_name_already_exists() {
        when(categoryRepository.existsByName("Antivirus")).thenReturn(true);

        var command = new CreateCategoryCommand("Antivirus",
                Map.of("fr", new CategoryTranslationDto("Antivirus", "desc")));
        Result<UUID> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("already exists");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void should_create_category_without_image() {
        when(categoryRepository.existsByName("Firewall")).thenReturn(false);

        var command = new CreateCategoryCommand("Firewall",
                Map.of("fr", new CategoryTranslationDto("Firewall", "desc")));
        Result<UUID> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        verify(categoryRepository).save(any(Category.class));
    }
}
