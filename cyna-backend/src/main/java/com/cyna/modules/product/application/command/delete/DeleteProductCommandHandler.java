package com.cyna.modules.product.application.command.delete;

import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class DeleteProductCommandHandler implements CommandHandler<DeleteProductCommand, Void> {

    private final ProductRepository productRepository;
    private final TransactionRunner transactionRunner;

    public DeleteProductCommandHandler(ProductRepository productRepository, TransactionRunner transactionRunner) {
        this.productRepository = productRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(DeleteProductCommand command) {
        if (!productRepository.existsById(command.id())) {
            return Result.failure("Product not found: " + command.id());
        }

        return transactionRunner.runReturning(() -> {
            productRepository.deleteById(command.id());
            return Result.success();
        });
    }
}
