package com.cyna.modules.product.application.command.deletecategory;

import com.cyna.modules.product.application.cache.ProductCacheNames;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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
    @Caching(evict = {
            @CacheEvict(cacheNames = ProductCacheNames.CATEGORY_LIST, allEntries = true),
            @CacheEvict(cacheNames = ProductCacheNames.CATEGORY_BY_ID, key = "#command.id()"),
            @CacheEvict(cacheNames = ProductCacheNames.PRODUCT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ProductCacheNames.PRODUCT_BY_ID, allEntries = true)
    })
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
