package com.cyna.modules.product.application.command.updatepromotion;

import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UpdatePromotionCommandHandler implements CommandHandler<UpdatePromotionCommand, UUID> {

    private final PromotionRepository promotionRepository;
    private final TransactionRunner transactionRunner;

    public UpdatePromotionCommandHandler(PromotionRepository promotionRepository,
                                         TransactionRunner transactionRunner) {
        this.promotionRepository = promotionRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<UUID> handle(UpdatePromotionCommand command) {
        Promotion existing = promotionRepository.findById(command.id()).orElse(null);
        if (existing == null) {
            return Result.failure("NOT_FOUND:Promotion not found: " + command.id());
        }

        Promotion updated;
        try {
            updated = existing.update(
                    command.discountPercent(),
                    command.marketingTextFr(),
                    command.marketingTextEn(),
                    command.startAt(),
                    command.endAt(),
                    command.enabled()
            );
        } catch (IllegalArgumentException e) {
            return Result.failure("VALIDATION_ERROR:" + e.getMessage());
        }

        if (updated.isEnabled() && promotionRepository.existsEnabledOverlappingWindow(
                updated.getProductId(),
                updated.getStartAt(),
                updated.getEndAt(),
                updated.getId()
        )) {
            return Result.failure("PROMOTION_OVERLAP:An enabled promotion already exists on this product for the same time range");
        }

        return transactionRunner.runReturning(() -> {
            promotionRepository.save(updated);
            return Result.success(updated.getId());
        });
    }
}

