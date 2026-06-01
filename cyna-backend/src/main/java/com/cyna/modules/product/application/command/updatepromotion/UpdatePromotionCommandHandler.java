package com.cyna.modules.product.application.command.updatepromotion;

import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UpdatePromotionCommandHandler implements CommandHandler<UpdatePromotionCommand, UUID> {

    private static final Logger log = LoggerFactory.getLogger(UpdatePromotionCommandHandler.class);

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
                    command.translations(),
                    command.startAt(),
                    command.endAt(),
                    command.enabled(),
                    command.showInCarousel(),
                    command.carouselOrder()
            );
        } catch (IllegalArgumentException e) {
            log.warn("Invalid promotion parameters: {}", e.getMessage());
            return Result.failure("VALIDATION_ERROR:Invalid promotion parameters");
        }

        if (updated.isEnabled() && promotionRepository.existsEnabledOverlappingWindow(
                updated.getProductId(),
                updated.getStartAt(),
                updated.getEndAt(),
                updated.getId()
        )) {
            return Result.failure("PROMOTION_OVERLAP:An enabled promotion already exists on this product for the same time range");
        }
        if (updated.isShowInCarousel()) {
            int maxAllowedOrder = Math.toIntExact(promotionRepository.countVisibleInCarousel(updated.getId()) + 1);
            if (updated.getCarouselOrder() == null || updated.getCarouselOrder() > maxAllowedOrder) {
                return Result.failure("CAROUSEL_ORDER_OUT_OF_RANGE:Carousel order must be between 1 and " + maxAllowedOrder);
            }
        }
        if (updated.isShowInCarousel() && promotionRepository.existsCarouselOrder(
                updated.getCarouselOrder(),
                updated.getId()
        )) {
            return Result.failure("CAROUSEL_ORDER_CONFLICT:Carousel order is already used by another promotion");
        }

        return transactionRunner.runReturning(() -> {
            promotionRepository.save(updated);
            return Result.success(updated.getId());
        });
    }
}
