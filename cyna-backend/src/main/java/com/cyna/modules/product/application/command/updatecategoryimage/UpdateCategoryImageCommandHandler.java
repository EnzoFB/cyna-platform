package com.cyna.modules.product.application.command.updatecategoryimage;

import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class UpdateCategoryImageCommandHandler implements CommandHandler<UpdateCategoryImageCommand, Void> {

    private final CategoryRepository categoryRepository;
    private final TransactionRunner transactionRunner;

    public UpdateCategoryImageCommandHandler(CategoryRepository categoryRepository,
                                             TransactionRunner transactionRunner) {
        this.categoryRepository = categoryRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(UpdateCategoryImageCommand command) {
        var existing = categoryRepository.findById(command.id());
        if (existing.isEmpty()) {
            return Result.failure("Category not found: " + command.id());
        }

        return transactionRunner.runReturning(() -> {
            var updated = existing.get().update(
                    existing.get().getName(),
                    existing.get().getDescription(),
                    command.image()
            );
            categoryRepository.save(updated.getValue());
            return Result.success(null);
        });
    }
}
