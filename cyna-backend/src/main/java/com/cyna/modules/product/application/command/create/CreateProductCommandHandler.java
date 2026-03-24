package com.cyna.modules.product.application.command.create;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateProductCommandHandler implements CommandHandler<CreateProductCommand, UUID> {

    private final ProductRepository productRepository;
    private final TransactionRunner transactionRunner;

    public CreateProductCommandHandler(ProductRepository productRepository, TransactionRunner transactionRunner) {
        this.productRepository = productRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<UUID> handle(CreateProductCommand command) {
        return transactionRunner.runReturning(() -> {
            Product product = Product.create(
                    command.name(),
                    command.category(),
                    command.priority(),
                    command.serviceDescription(),
                    command.technicalDescription(),
                    Money.of(command.monthlyPrice(), command.currency()),
                    Money.of(command.annualPrice(), command.currency())
            );

            productRepository.save(product);
            return Result.success(product.getId());
        });
    }
}
