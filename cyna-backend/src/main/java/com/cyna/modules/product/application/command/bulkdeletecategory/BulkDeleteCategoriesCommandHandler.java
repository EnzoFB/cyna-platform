package com.cyna.modules.product.application.command.bulkdeletecategory;

import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class BulkDeleteCategoriesCommandHandler implements CommandHandler<BulkDeleteCategoriesCommand, Void> {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final TransactionRunner transactionRunner;

    public BulkDeleteCategoriesCommandHandler(CategoryRepository categoryRepository,
                                              ProductRepository productRepository,
                                              TransactionRunner transactionRunner) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(BulkDeleteCategoriesCommand command) {
        List<UUID> ids = command.ids();
        if (ids == null || ids.isEmpty()) {
            return Result.failure("No category IDs provided");
        }

        if (productRepository.existsByCategoryIdIn(ids)) {
            return Result.failure("HAS_PRODUCTS:Une ou plusieurs catégories sélectionnées contiennent des produits et ne peuvent pas être supprimées.");
        }

        return transactionRunner.runReturning(() -> {
            categoryRepository.deleteAllByIds(ids);
            return Result.success();
        });
    }
}
