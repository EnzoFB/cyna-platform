package com.cyna.modules.product.application.command.deletecategory;

import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class DeleteCategoryCommandHandler implements CommandHandler<DeleteCategoryCommand, Void> {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final TransactionRunner transactionRunner;

    public DeleteCategoryCommandHandler(CategoryRepository categoryRepository,
                                        ProductRepository productRepository,
                                        TransactionRunner transactionRunner) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(DeleteCategoryCommand command) {
        if (categoryRepository.findById(command.id()).isEmpty()) {
            return Result.failure("Category not found: " + command.id());
        }

        long productCount = productRepository.countByCategoryId(command.id());
        if (productCount > 0) {
            return Result.failure("HAS_PRODUCTS:Cette catégorie contient " + productCount + " produit(s) et ne peut pas être supprimée.");
        }

        return transactionRunner.runReturning(() -> {
            categoryRepository.deleteById(command.id());
            return Result.success(null);
        });
    }
}
