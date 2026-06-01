package com.cyna.modules.product.application.command.createpromotion;

import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreatePromotionCommandHandler implements CommandHandler<CreatePromotionCommand, UUID> {

    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final OfferCarouselSettingsRepository settingsRepository;
    private final TransactionRunner transactionRunner;

    public CreatePromotionCommandHandler(ProductRepository productRepository,
                                         PromotionRepository promotionRepository,
                                         OfferCarouselSettingsRepository settingsRepository,
                                         TransactionRunner transactionRunner) {
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.settingsRepository = settingsRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<UUID> handle(CreatePromotionCommand command) {
        if (!productRepository.existsById(command.productId())) {
            return Result.failure("PRODUCT_NOT_FOUND:Product not found: " + command.productId());
        }

        Promotion promotion;
        try {
            promotion = Promotion.create(
                    command.productId(),
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

        if (promotion.isEnabled() && promotionRepository.existsEnabledOverlappingWindow(
                promotion.getProductId(),
                promotion.getStartAt(),
                promotion.getEndAt(),
                null
        )) {
            return Result.failure("PROMOTION_OVERLAP:An enabled promotion already exists on this product for the same time range");
        }

        if (promotion.isShowInCarousel()) {
            int maxSlides = settingsRepository.find()
                    .map(OfferCarouselSettings::getMaxSlides)
                    .orElse(OfferCarouselSettings.DEFAULT_MAX_SLIDES);
            long currentCount = promotionRepository.countVisibleInCarousel(null);
            if (currentCount >= maxSlides) {
                return Result.failure("CAROUSEL_LIMIT_EXCEEDED:Cannot add more than " + maxSlides + " slides to the carousel");
            }
            int maxAllowedOrder = Math.toIntExact(currentCount + 1);
            if (promotion.getCarouselOrder() == null || promotion.getCarouselOrder() > maxAllowedOrder) {
                return Result.failure("CAROUSEL_ORDER_OUT_OF_RANGE:Carousel order must be between 1 and " + maxAllowedOrder);
            }
        }
        if (promotion.isShowInCarousel() && promotionRepository.existsCarouselOrder(
                promotion.getCarouselOrder(),
                null
        )) {
            return Result.failure("CAROUSEL_ORDER_CONFLICT:Carousel order is already used by another promotion");
        }

        return transactionRunner.runReturning(() -> {
            promotionRepository.save(promotion);
            return Result.success(promotion.getId());
        });
    }
}
