package com.cyna.modules.product.application.command.deletepromotion;

import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class DeletePromotionCommandHandler implements CommandHandler<DeletePromotionCommand, Void> {

    private final PromotionRepository promotionRepository;
    private final TransactionRunner transactionRunner;

    public DeletePromotionCommandHandler(PromotionRepository promotionRepository,
                                         TransactionRunner transactionRunner) {
        this.promotionRepository = promotionRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(DeletePromotionCommand command) {
        if (promotionRepository.findById(command.id()).isEmpty()) {
            return Result.failure("NOT_FOUND:Promotion not found: " + command.id());
        }

        return transactionRunner.runReturning(() -> {
            promotionRepository.deleteById(command.id());
            return Result.success();
        });
    }
}

