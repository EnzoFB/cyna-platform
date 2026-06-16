package com.cyna.modules.product.application.command.bulkdelete;

import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class BulkDeleteProductsCommandHandler implements CommandHandler<BulkDeleteProductsCommand, Void> {

    private final ProductRepository productRepository;
    private final TransactionRunner transactionRunner;

    public BulkDeleteProductsCommandHandler(ProductRepository productRepository, TransactionRunner transactionRunner) {
        this.productRepository = productRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(BulkDeleteProductsCommand command) {
        List<UUID> ids = command.ids();
        if (ids == null || ids.isEmpty()) {
            return Result.failure("No product IDs provided");
        }

        return transactionRunner.runReturning(() -> {
            productRepository.deleteAllByIds(ids);
            return Result.success();
        });
    }
}
