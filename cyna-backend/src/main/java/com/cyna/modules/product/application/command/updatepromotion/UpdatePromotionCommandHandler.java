package com.cyna.modules.product.application.command.updatepromotion;

import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UpdatePromotionCommandHandler implements CommandHandler<UpdatePromotionCommand, UUID> {

    private final PromotionRepository promotionRepository;
    private final OfferCarouselSettingsRepository settingsRepository;
    private final TransactionRunner transactionRunner;

    public UpdatePromotionCommandHandler(PromotionRepository promotionRepository,
                                         OfferCarouselSettingsRepository settingsRepository,
                                         TransactionRunner transactionRunner) {
        this.promotionRepository = promotionRepository;
        this.settingsRepository = settingsRepository;
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

        // Only check limit when adding a new promo to carousel (not when it was already there)
        if (updated.isShowInCarousel() && !existing.isShowInCarousel()) {
            int maxSlides = settingsRepository.find()
                    .map(OfferCarouselSettings::getMaxSlides)
                    .orElse(OfferCarouselSettings.DEFAULT_MAX_SLIDES);
            long currentCount = promotionRepository.countVisibleInCarousel(updated.getId());
            if (currentCount >= maxSlides) {
                return Result.failure("CAROUSEL_LIMIT_EXCEEDED:Cannot add more than " + maxSlides + " slides to the carousel");
            }
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
