package com.cyna.modules.product.application.command.update;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class UpdateProductCommandHandler implements CommandHandler<UpdateProductCommand, UUID> {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRunner transactionRunner;

    public UpdateProductCommandHandler(ProductRepository productRepository,
                                       CategoryRepository categoryRepository,
                                       TransactionRunner transactionRunner) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<UUID> handle(UpdateProductCommand command) {
        Optional<Product> existingOpt = productRepository.findById(command.id());
        if (existingOpt.isEmpty()) {
            return Result.failure("Product not found: " + command.id());
        }

        if (categoryRepository.findById(command.categoryId()).isEmpty()) {
            return Result.failure("Category not found: " + command.categoryId());
        }

        Product existing = existingOpt.get();

        return transactionRunner.runReturning(() -> {
            Product updated = Product.reconstitute(
                    existing.getId(),
                    command.name(),
                    command.nameEn(),
                    command.categoryId(),
                    command.priorityLevel(),
                    command.serviceDescription(),
                    command.serviceDescriptionEn(),
                    command.technicalDescription(),
                    command.technicalDescriptionEn(),
                    command.monthlyPrice(),
                    command.annualPrice(),
                    command.currency(),
                    command.isPublished(),
                    command.isAvailable(),
                    command.freeTrialDays(),
                    command.highlightPoints(),
                    command.highlightPointsEn(),
                    existing.getCreatedAt(),
                    Instant.now()
            );

            productRepository.save(updated);
            return Result.success(updated.getId());
        });
    }
}
