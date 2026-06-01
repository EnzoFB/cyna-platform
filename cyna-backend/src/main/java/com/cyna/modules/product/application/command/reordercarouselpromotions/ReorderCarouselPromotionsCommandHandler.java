package com.cyna.modules.product.application.command.reordercarouselpromotions;

import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReorderCarouselPromotionsCommandHandler
        implements CommandHandler<ReorderCarouselPromotionsCommand, Void> {

    private final PromotionRepository promotionRepository;
    private final OfferCarouselSettingsRepository settingsRepository;
    private final TransactionRunner transactionRunner;

    public ReorderCarouselPromotionsCommandHandler(PromotionRepository promotionRepository,
                                                   OfferCarouselSettingsRepository settingsRepository,
                                                   TransactionRunner transactionRunner) {
        this.promotionRepository = promotionRepository;
        this.settingsRepository = settingsRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(ReorderCarouselPromotionsCommand command) {
        List<UUID> orderedIds = command.orderedIds();

        if (orderedIds == null || orderedIds.isEmpty()) {
            return Result.failure("VALIDATION_ERROR:orderedIds must not be empty");
        }

        // Check against carousel limit
        int maxSlides = settingsRepository.find()
                .map(OfferCarouselSettings::getMaxSlides)
                .orElse(OfferCarouselSettings.DEFAULT_MAX_SLIDES);

        if (orderedIds.size() > maxSlides) {
            return Result.failure("CAROUSEL_LIMIT_EXCEEDED:Cannot have more than " + maxSlides + " slides in the carousel");
        }

        // Load all current carousel promotions and index them
        Map<UUID, Promotion> carouselById = promotionRepository.findAllInCarousel().stream()
                .collect(Collectors.toMap(Promotion::getId, Function.identity()));

        // Validate all submitted IDs are actually in the carousel
        for (UUID id : orderedIds) {
            if (!carouselById.containsKey(id)) {
                return Result.failure("NOT_FOUND:Promotion " + id + " is not in the carousel");
            }
        }

        // Validate no duplicates
        long distinctCount = orderedIds.stream().distinct().count();
        if (distinctCount != orderedIds.size()) {
            return Result.failure("VALIDATION_ERROR:orderedIds contains duplicate entries");
        }

        // Assign new carouselOrder (1-based) and rebuild each promotion
        List<Promotion> updated = new ArrayList<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            Promotion p = carouselById.get(orderedIds.get(i));
            Promotion reordered = p.update(
                    p.getDiscountPercent(),
                    p.getTranslations(),
                    p.getStartAt(),
                    p.getEndAt(),
                    p.isEnabled(),
                    true,
                    i + 1
            );
            updated.add(reordered);
        }

        return transactionRunner.runReturning(() -> {
            promotionRepository.saveAll(updated);
            return Result.success(null);
        });
    }
}
