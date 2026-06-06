package com.cyna.modules.product.application.command.addtocaousel;

import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.repository.CarouselSlotRepository;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class AddToCarouselCommandHandler implements CommandHandler<AddToCarouselCommand, Void> {

    private final PromotionRepository promotionRepository;
    private final CarouselSlotRepository carouselSlotRepository;
    private final OfferCarouselSettingsRepository settingsRepository;
    private final TransactionRunner transactionRunner;

    public AddToCarouselCommandHandler(PromotionRepository promotionRepository,
                                       CarouselSlotRepository carouselSlotRepository,
                                       OfferCarouselSettingsRepository settingsRepository,
                                       TransactionRunner transactionRunner) {
        this.promotionRepository = promotionRepository;
        this.carouselSlotRepository = carouselSlotRepository;
        this.settingsRepository = settingsRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(AddToCarouselCommand command) {
        if (promotionRepository.findById(command.promotionId()).isEmpty()) {
            return Result.failure("NOT_FOUND:Promotion not found: " + command.promotionId());
        }

        if (carouselSlotRepository.existsByPromotionId(command.promotionId())) {
            return Result.failure("ALREADY_IN_CAROUSEL:Promotion is already in the carousel");
        }

        int maxSlides = settingsRepository.find()
                .map(OfferCarouselSettings::getMaxSlides)
                .orElse(OfferCarouselSettings.DEFAULT_MAX_SLIDES);

        if (carouselSlotRepository.count() >= maxSlides) {
            return Result.failure("CAROUSEL_LIMIT_EXCEEDED:Cannot add more than " + maxSlides + " slides to the carousel");
        }

        return transactionRunner.runReturning(() -> {
            carouselSlotRepository.add(command.promotionId());
            return Result.success(null);
        });
    }
}
