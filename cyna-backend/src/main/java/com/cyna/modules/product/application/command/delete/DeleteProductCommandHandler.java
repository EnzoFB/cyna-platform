package com.cyna.modules.product.application.command.delete;

import com.cyna.modules.product.application.cache.ProductCacheNames;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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
    @Caching(evict = {
            @CacheEvict(cacheNames = ProductCacheNames.PRODUCT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ProductCacheNames.PRODUCT_BY_ID, key = "#command.id()")
    })
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
