package com.cyna.modules.product.application.command.updatepromotion;

import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.model.PromotionTranslation;
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
class UpdatePromotionCommandHandlerTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private OfferCarouselSettingsRepository settingsRepository;

    private UpdatePromotionCommandHandler handler;

    private static final UUID PRODUCT_ID    = UUID.randomUUID();
    private static final UUID PROMOTION_ID  = UUID.randomUUID();

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    private static final Instant START = Instant.now().minusSeconds(3600);
    private static final Instant END   = Instant.now().plusSeconds(86400);

    private static final Map<String, PromotionTranslation> TRANSLATIONS = Map.of(
            "fr", new PromotionTranslation("Promo FR"),
            "en", new PromotionTranslation("Promo EN")
    );

    /** Creates an existing promotion not in the carousel. */
    private Promotion existingNotInCarousel() {
        return Promotion.reconstitute(
                PROMOTION_ID, PRODUCT_ID, 10, TRANSLATIONS, START, END,
                true, false, null, Instant.now(), Instant.now());
    }

    /** Creates an existing promotion already in the carousel at order 1. */
    private Promotion existingInCarousel() {
        return Promotion.reconstitute(
                PROMOTION_ID, PRODUCT_ID, 10, TRANSLATIONS, START, END,
                true, true, 1, Instant.now(), Instant.now());
    }

    @BeforeEach
    void setUp() {
        handler = new UpdatePromotionCommandHandler(promotionRepository, settingsRepository, transactionRunner);
    }

    @Test
    void should_update_promotion_successfully() {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(existingNotInCarousel()));
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(false);

        var command = new UpdatePromotionCommand(PROMOTION_ID, 20, TRANSLATIONS, START, END, true, false, null);
        Result<UUID> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(PROMOTION_ID);
        verify(promotionRepository).save(any());
    }

    @Test
    void should_fail_when_promotion_not_found() {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.empty());

        Result<UUID> result = handler.handle(
                new UpdatePromotionCommand(PROMOTION_ID, 10, TRANSLATIONS, START, END, true, false, null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("NOT_FOUND");
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void should_fail_when_carousel_limit_exceeded_on_adding_to_carousel() {
        // Promotion was NOT in carousel, now being added → limit check applies
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(existingNotInCarousel()));
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(false);
        OfferCarouselSettings settings = OfferCarouselSettings.reconstitute(
                Map.of(), 3, Instant.now(), Instant.now());
        when(settingsRepository.find()).thenReturn(Optional.of(settings));
        when(promotionRepository.countVisibleInCarousel(PROMOTION_ID)).thenReturn(3L); // full

        Result<UUID> result = handler.handle(
                new UpdatePromotionCommand(PROMOTION_ID, 10, TRANSLATIONS, START, END, true, true, 4));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("CAROUSEL_LIMIT_EXCEEDED");
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void should_not_check_carousel_limit_when_already_in_carousel() {
        // Promotion was already in carousel → no limit check (slot already reserved)
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(existingInCarousel()));
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(false);
        when(promotionRepository.existsCarouselOrder(any(), any())).thenReturn(false);

        Result<UUID> result = handler.handle(
                new UpdatePromotionCommand(PROMOTION_ID, 25, TRANSLATIONS, START, END, true, true, 1));

        assertThat(result.isSuccess()).isTrue();
        verify(settingsRepository, never()).find();
        verify(promotionRepository, never()).countVisibleInCarousel(any());
    }

    @Test
    void should_fail_when_carousel_order_conflicts_with_another_promotion() {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(existingNotInCarousel()));
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(false);
        when(settingsRepository.find()).thenReturn(Optional.empty());
        when(promotionRepository.countVisibleInCarousel(PROMOTION_ID)).thenReturn(0L);
        when(promotionRepository.existsCarouselOrder(2, PROMOTION_ID)).thenReturn(true);

        Result<UUID> result = handler.handle(
                new UpdatePromotionCommand(PROMOTION_ID, 10, TRANSLATIONS, START, END, true, true, 2));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("CAROUSEL_ORDER_CONFLICT");
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void should_fail_when_enabled_promotion_overlaps() {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(existingNotInCarousel()));
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(true);

        Result<UUID> result = handler.handle(
                new UpdatePromotionCommand(PROMOTION_ID, 10, TRANSLATIONS, START, END, true, false, null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("PROMOTION_OVERLAP");
        verify(promotionRepository, never()).save(any());
    }
}
