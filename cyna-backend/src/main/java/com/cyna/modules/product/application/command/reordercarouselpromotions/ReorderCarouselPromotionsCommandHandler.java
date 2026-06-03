package com.cyna.modules.product.application.command.reordercarouselpromotions;

import com.cyna.modules.product.domain.model.CarouselSlot;
import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.repository.CarouselSlotRepository;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ReorderCarouselPromotionsCommandHandler
        implements CommandHandler<ReorderCarouselPromotionsCommand, Void> {

    private final CarouselSlotRepository carouselSlotRepository;
    private final OfferCarouselSettingsRepository settingsRepository;
    private final TransactionRunner transactionRunner;

    public ReorderCarouselPromotionsCommandHandler(CarouselSlotRepository carouselSlotRepository,
                                                   OfferCarouselSettingsRepository settingsRepository,
                                                   TransactionRunner transactionRunner) {
        this.carouselSlotRepository = carouselSlotRepository;
        this.settingsRepository = settingsRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(ReorderCarouselPromotionsCommand command) {
        List<UUID> orderedIds = command.orderedIds();

        if (orderedIds == null || orderedIds.isEmpty()) {
            return Result.failure("VALIDATION_ERROR:orderedIds must not be empty");
        }

        int maxSlides = settingsRepository.find()
                .map(OfferCarouselSettings::getMaxSlides)
                .orElse(OfferCarouselSettings.DEFAULT_MAX_SLIDES);

        if (orderedIds.size() > maxSlides) {
            return Result.failure("CAROUSEL_LIMIT_EXCEEDED:Cannot have more than " + maxSlides + " slides in the carousel");
        }

        long distinctCount = orderedIds.stream().distinct().count();
        if (distinctCount != orderedIds.size()) {
            return Result.failure("VALIDATION_ERROR:orderedIds contains duplicate entries");
        }

        Set<UUID> currentSlotPromotionIds = carouselSlotRepository.findAll().stream()
                .map(CarouselSlot::promotionId)
                .collect(Collectors.toSet());

        for (UUID id : orderedIds) {
            if (!currentSlotPromotionIds.contains(id)) {
                return Result.failure("NOT_FOUND:Promotion " + id + " is not in the carousel");
            }
        }

        return transactionRunner.runReturning(() -> {
            carouselSlotRepository.reorder(orderedIds);
            return Result.success(null);
        });
    }
}
