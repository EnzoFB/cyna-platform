package com.cyna.modules.product.application.command.updatecategory;

import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UpdateCategoryCommandHandler implements CommandHandler<UpdateCategoryCommand, UUID> {

    private final CategoryRepository categoryRepository;
    private final TransactionRunner transactionRunner;

    public UpdateCategoryCommandHandler(CategoryRepository categoryRepository,
                                        TransactionRunner transactionRunner) {
        this.categoryRepository = categoryRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<UUID> handle(UpdateCategoryCommand command) {
        var existing = categoryRepository.findById(command.id());
        if (existing.isEmpty()) {
            return Result.failure("Category not found: " + command.id());
        }

        return transactionRunner.runReturning(() -> {
            var updated = existing.get().update(command.name(), command.description(), null);
            if (updated.isFailure()) {
                return Result.failure(updated.getError());
            }
            categoryRepository.save(updated.getValue());
            return Result.success(updated.getValue().getId());
        });
    }
}
