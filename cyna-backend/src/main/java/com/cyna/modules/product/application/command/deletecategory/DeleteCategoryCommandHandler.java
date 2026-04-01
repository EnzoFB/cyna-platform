package com.cyna.modules.product.application.command.deletecategory;

import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class DeleteCategoryCommandHandler implements CommandHandler<DeleteCategoryCommand, Void> {

    private final CategoryRepository categoryRepository;
    private final TransactionRunner transactionRunner;

    public DeleteCategoryCommandHandler(CategoryRepository categoryRepository,
                                        TransactionRunner transactionRunner) {
        this.categoryRepository = categoryRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(DeleteCategoryCommand command) {
        var existing = categoryRepository.findById(command.id());
        if (existing.isEmpty()) {
            return Result.failure("Category not found: " + command.id());
        }

        return transactionRunner.runReturning(() -> {
            var deactivated = existing.get().deactivate();
            if (deactivated.isFailure()) {
                return Result.failure(deactivated.getError());
            }
            categoryRepository.save(deactivated.getValue());
            return Result.success(null);
        });
    }
}
