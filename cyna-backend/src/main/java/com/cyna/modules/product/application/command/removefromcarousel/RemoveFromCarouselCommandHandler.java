package com.cyna.modules.product.application.command.removefromcarousel;

import com.cyna.modules.product.domain.model.CarouselSlot;
import com.cyna.modules.product.domain.repository.CarouselSlotRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RemoveFromCarouselCommandHandler implements CommandHandler<RemoveFromCarouselCommand, Void> {

    private final PromotionRepository promotionRepository;
    private final CarouselSlotRepository carouselSlotRepository;
    private final TransactionRunner transactionRunner;

    public RemoveFromCarouselCommandHandler(PromotionRepository promotionRepository,
                                            CarouselSlotRepository carouselSlotRepository,
                                            TransactionRunner transactionRunner) {
        this.promotionRepository = promotionRepository;
        this.carouselSlotRepository = carouselSlotRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(RemoveFromCarouselCommand command) {
        if (promotionRepository.findById(command.promotionId()).isEmpty()) {
            return Result.failure("NOT_FOUND:Promotion not found: " + command.promotionId());
        }

        return transactionRunner.runReturning(() -> {
            carouselSlotRepository.remove(command.promotionId());
            // Compact remaining slots so positions stay consecutive (no gaps)
            List<UUID> remaining = carouselSlotRepository.findAll().stream()
                    .map(CarouselSlot::promotionId)
                    .toList();
            if (!remaining.isEmpty()) {
                carouselSlotRepository.reorder(remaining);
            }
            return Result.success(null);
        });
    }
}
