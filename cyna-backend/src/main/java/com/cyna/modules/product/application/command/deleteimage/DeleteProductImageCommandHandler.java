package com.cyna.modules.product.application.command.deleteimage;

import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class DeleteProductImageCommandHandler implements CommandHandler<DeleteProductImageCommand, Void> {

    private final ProductImageRepository productImageRepository;
    private final TransactionRunner transactionRunner;

    public DeleteProductImageCommandHandler(ProductImageRepository productImageRepository,
                                            TransactionRunner transactionRunner) {
        this.productImageRepository = productImageRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(DeleteProductImageCommand command) {
        var existing = productImageRepository.findById(command.imageId());
        if (existing.isEmpty()) {
            return Result.failure("Image not found: " + command.imageId());
        }
        if (!existing.get().getProductId().equals(command.productId())) {
            return Result.failure("Image does not belong to product: " + command.productId());
        }
        int deletedOrder = existing.get().getDisplayOrder();
        return transactionRunner.runReturning(() -> {
            productImageRepository.deleteById(command.imageId());
            productImageRepository.decrementDisplayOrderAfter(command.productId(), deletedOrder);
            return Result.success(null);
        });
    }
}
