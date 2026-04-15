package com.cyna.modules.product.application.command.create;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateProductCommandHandler implements CommandHandler<CreateProductCommand, UUID> {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRunner transactionRunner;

    public CreateProductCommandHandler(ProductRepository productRepository,
                                       CategoryRepository categoryRepository,
                                       TransactionRunner transactionRunner) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<UUID> handle(CreateProductCommand command) {
        if (categoryRepository.findById(command.categoryId()).isEmpty()) {
            return Result.failure("Category not found: " + command.categoryId());
        }

        return transactionRunner.runReturning(() -> {
            Product product = Product.create(
                    command.name(),
                    command.categoryId(),
                    command.priorityLevel(),
                    command.serviceDescription(),
                    command.technicalDescription(),
                    command.monthlyPrice(),
                    command.annualPrice(),
                    command.currency(),
                    command.freeTrialDays(),
                    command.highlightPoints()
            );

            productRepository.save(product);
            return Result.success(product.getId());
        });
    }
}
