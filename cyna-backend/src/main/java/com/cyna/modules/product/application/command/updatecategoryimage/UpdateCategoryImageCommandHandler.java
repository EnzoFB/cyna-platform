package com.cyna.modules.product.application.command.updatecategoryimage;

import com.cyna.modules.product.application.cache.ProductCacheNames;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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
    @Caching(evict = {
            @CacheEvict(cacheNames = ProductCacheNames.CATEGORY_LIST, allEntries = true),
            @CacheEvict(cacheNames = ProductCacheNames.CATEGORY_BY_ID, key = "#command.id()")
    })
    public Result<Void> handle(UpdateCategoryImageCommand command) {
        var existing = categoryRepository.findById(command.id());
        if (existing.isEmpty()) {
            return Result.failure("Category not found: " + command.id());
        }

        return transactionRunner.runReturning(() -> {
            var updated = existing.get().update(
                    existing.get().getName(),
                    existing.get().getFullName(),
                    existing.get().getDescription(),
                    command.image()
            );
            categoryRepository.save(updated.getValue());
            return Result.success(null);
        });
    }
}
