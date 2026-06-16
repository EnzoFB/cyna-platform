package com.cyna.modules.product.application.command.addimage;

import com.cyna.modules.product.domain.model.ProductImage;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AddProductImageCommandHandler implements CommandHandler<AddProductImageCommand, UUID> {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final TransactionRunner transactionRunner;

    public AddProductImageCommandHandler(ProductRepository productRepository,
                                         ProductImageRepository productImageRepository,
                                         TransactionRunner transactionRunner) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<UUID> handle(AddProductImageCommand command) {
        if (productRepository.findById(command.productId()).isEmpty()) {
            return Result.failure("Product not found: " + command.productId());
        }
        return transactionRunner.runReturning(() -> {
            int nextOrder = productImageRepository.maxDisplayOrderByProductId(command.productId()) + 1;
            ProductImage image = ProductImage.create(
                    command.productId(),
                    command.imageData(),
                    command.mimeType(),
                    nextOrder
            );
            ProductImage saved = productImageRepository.save(image);
            return Result.success(saved.getId());
        });
    }
}
