package com.cyna.modules.product.application.command.addtocaousel;

import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.model.PromotionTranslation;
import com.cyna.modules.product.domain.repository.CarouselSlotRepository;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddToCarouselCommandHandlerTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private CarouselSlotRepository carouselSlotRepository;
    @Mock private OfferCarouselSettingsRepository settingsRepository;

    private AddToCarouselCommandHandler handler;

    private static final UUID PROMOTION_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID   = UUID.randomUUID();

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    private static final Map<String, PromotionTranslation> TRANSLATIONS = Map.of(
            "fr", new PromotionTranslation("FR"), "en", new PromotionTranslation("EN"));

    private Promotion promotion() {
        Instant now = Instant.now();
        return Promotion.reconstitute(PROMOTION_ID, PRODUCT_ID, 10, TRANSLATIONS,
                now.minusSeconds(3600), now.plusSeconds(86400), true, now, now);
    }

    @BeforeEach
    void setUp() {
        handler = new AddToCarouselCommandHandler(
                promotionRepository, carouselSlotRepository, settingsRepository, transactionRunner);
    }

    @Test
    void should_add_to_carousel_successfully() {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
        when(carouselSlotRepository.existsByPromotionId(PROMOTION_ID)).thenReturn(false);
        when(settingsRepository.find()).thenReturn(Optional.empty());
        when(carouselSlotRepository.count()).thenReturn(2);

        Result<Void> result = handler.handle(new AddToCarouselCommand(PROMOTION_ID));

        assertThat(result.isSuccess()).isTrue();
        verify(carouselSlotRepository).add(PROMOTION_ID);
    }

    @Test
    void should_fail_when_promotion_not_found() {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.empty());

        Result<Void> result = handler.handle(new AddToCarouselCommand(PROMOTION_ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("NOT_FOUND");
        verify(carouselSlotRepository, never()).add(any());
    }

    @Test
    void should_fail_when_already_in_carousel() {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
        when(carouselSlotRepository.existsByPromotionId(PROMOTION_ID)).thenReturn(true);

        Result<Void> result = handler.handle(new AddToCarouselCommand(PROMOTION_ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("ALREADY_IN_CAROUSEL");
        verify(carouselSlotRepository, never()).add(any());
    }

    @Test
    void should_fail_when_carousel_is_full() {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
        when(carouselSlotRepository.existsByPromotionId(PROMOTION_ID)).thenReturn(false);
        OfferCarouselSettings settings = OfferCarouselSettings.reconstitute(Map.of(), 3, Instant.now(), Instant.now());
        when(settingsRepository.find()).thenReturn(Optional.of(settings));
        when(carouselSlotRepository.count()).thenReturn(3);

        Result<Void> result = handler.handle(new AddToCarouselCommand(PROMOTION_ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("CAROUSEL_LIMIT_EXCEEDED");
        verify(carouselSlotRepository, never()).add(any());
    }
}
