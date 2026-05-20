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
            var category = existing.get();

            var updateResult = category.update(command.name(), command.translations(), null);
            if (updateResult.isFailure()) {
                return Result.failure(updateResult.getError());
            }
            var updated = updateResult.getValue();

            if (command.active() && !category.isActive()) {
                var activateResult = updated.activate();
                if (activateResult.isFailure()) {
                    return Result.failure(activateResult.getError());
                }
                updated = activateResult.getValue();
            } else if (!command.active() && category.isActive()) {
                var deactivateResult = updated.deactivate();
                if (deactivateResult.isFailure()) {
                    return Result.failure(deactivateResult.getError());
                }
                updated = deactivateResult.getValue();
            }

            categoryRepository.save(updated);
            return Result.success(updated.getId());
        });
    }
}
