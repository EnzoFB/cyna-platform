package com.cyna.modules.product.application.command.update;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class UpdateProductCommandHandler implements CommandHandler<UpdateProductCommand, UUID> {

    private final ProductRepository productRepository;
    private final TransactionRunner transactionRunner;

    public UpdateProductCommandHandler(ProductRepository productRepository, TransactionRunner transactionRunner) {
        this.productRepository = productRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<UUID> handle(UpdateProductCommand command) {
        Optional<Product> existingOpt = productRepository.findById(command.id());
        if (existingOpt.isEmpty()) {
            return Result.failure("Product not found: " + command.id());
        }

        Product existing = existingOpt.get();

        return transactionRunner.runReturning(() -> {
            Product updated = Product.reconstitute(
                    existing.getId(),
                    command.name(),
                    command.category(),
                    command.priority(),
                    command.serviceDescription(),
                    command.technicalDescription(),
                    Money.of(command.monthlyPrice(), command.currency()),
                    Money.of(command.annualPrice(), command.currency()),
                    existing.getStatus(),
                    existing.getCreatedAt(),
                    Instant.now()
            );

            productRepository.save(updated);
            return Result.success(updated.getId());
        });
    }
}
