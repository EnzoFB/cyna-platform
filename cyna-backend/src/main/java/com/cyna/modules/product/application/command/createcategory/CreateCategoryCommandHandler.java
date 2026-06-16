package com.cyna.modules.product.application.command.createcategory;

import com.cyna.modules.product.application.translation.CategoryTranslationDto;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateCategoryCommandHandler implements CommandHandler<CreateCategoryCommand, UUID> {

    private final CategoryRepository categoryRepository;
    private final TransactionRunner transactionRunner;

    public CreateCategoryCommandHandler(CategoryRepository categoryRepository,
                                        TransactionRunner transactionRunner) {
        this.categoryRepository = categoryRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<UUID> handle(CreateCategoryCommand command) {
        if (categoryRepository.existsByName(command.name())) {
            return Result.failure("Category name already exists");
        }

        return transactionRunner.runReturning(() -> {
            Category category = Category.create(
                    command.name(),
                    CategoryTranslationDto.toDomainMap(command.translations()),
                    null);
            categoryRepository.save(category);
            return Result.success(category.getId());
        });
    }
}
