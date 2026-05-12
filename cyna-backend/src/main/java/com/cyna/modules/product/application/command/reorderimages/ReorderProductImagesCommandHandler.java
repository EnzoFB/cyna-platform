package com.cyna.modules.product.application.command.reorderimages;

import com.cyna.modules.product.domain.model.ProductImage;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ReorderProductImagesCommandHandler implements CommandHandler<ReorderProductImagesCommand, Void> {

    private final ProductImageRepository productImageRepository;
    private final TransactionRunner transactionRunner;

    public ReorderProductImagesCommandHandler(ProductImageRepository productImageRepository,
                                              TransactionRunner transactionRunner) {
        this.productImageRepository = productImageRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(ReorderProductImagesCommand command) {
        List<ProductImage> existing = productImageRepository.findByProductId(command.productId());
        Set<UUID> existingIds = existing.stream().map(ProductImage::getId).collect(Collectors.toSet());

        for (UUID id : command.orderedImageIds()) {
            if (!existingIds.contains(id)) {
                return Result.failure("Image not found for this product: " + id);
            }
        }

        if (command.orderedImageIds().size() != existingIds.size()) {
            return Result.failure("orderedImageIds must contain all image IDs for this product");
        }

        return transactionRunner.runReturning(() -> {
            List<UUID> ordered = command.orderedImageIds();
            int n = ordered.size();
            // Pass 1 : temporary values >= n to avoid conflicts with final values [0..n-1]
            for (int i = 0; i < n; i++) {
                productImageRepository.updateDisplayOrder(ordered.get(i), n + i);
            }
            // Pass 2 : final values
            for (int i = 0; i < n; i++) {
                productImageRepository.updateDisplayOrder(ordered.get(i), i);
            }
            return Result.success(null);
        });
    }
}
