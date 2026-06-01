package com.cyna.modules.product.application.command.createpromotion;

import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.model.PromotionTranslation;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
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
class CreatePromotionCommandHandlerTest {

    @Mock private ProductRepository productRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private OfferCarouselSettingsRepository settingsRepository;

    private CreatePromotionCommandHandler handler;

    private static final UUID PRODUCT_ID = UUID.randomUUID();

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

    @BeforeEach
    void setUp() {
        handler = new CreatePromotionCommandHandler(
                productRepository, promotionRepository, settingsRepository, transactionRunner);
    }

    @Test
    void should_create_promotion_successfully() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(false);
        when(settingsRepository.find()).thenReturn(Optional.empty());
        when(promotionRepository.countVisibleInCarousel(null)).thenReturn(0L);
        when(promotionRepository.existsCarouselOrder(any(), any())).thenReturn(false);

        var command = new CreatePromotionCommand(PRODUCT_ID, 20, TRANSLATIONS, START, END, true, true, 1);
        Result<UUID> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isNotNull();
        verify(promotionRepository).save(any());
    }

    @Test
    void should_create_promotion_without_carousel_skipping_limit_check() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(false);

        var command = new CreatePromotionCommand(PRODUCT_ID, 10, TRANSLATIONS, START, END, true, false, null);
        Result<UUID> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        verify(settingsRepository, never()).find();
        verify(promotionRepository, never()).countVisibleInCarousel(any());
    }

    @Test
    void should_fail_when_product_not_found() {
        UUID unknownId = UUID.randomUUID();
        when(productRepository.existsById(unknownId)).thenReturn(false);

        Result<UUID> result = handler.handle(
                new CreatePromotionCommand(unknownId, 10, TRANSLATIONS, START, END, true, false, null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("PRODUCT_NOT_FOUND");
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void should_fail_when_enabled_promotion_overlaps() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(true);

        Result<UUID> result = handler.handle(
                new CreatePromotionCommand(PRODUCT_ID, 10, TRANSLATIONS, START, END, true, false, null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("PROMOTION_OVERLAP");
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void should_fail_when_carousel_is_full() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(false);
        // Settings returns maxSlides = 3; carousel already has 3 items
        OfferCarouselSettings settings = OfferCarouselSettings.reconstitute(
                Map.of(), 3, Instant.now(), Instant.now());
        when(settingsRepository.find()).thenReturn(Optional.of(settings));
        when(promotionRepository.countVisibleInCarousel(null)).thenReturn(3L);

        Result<UUID> result = handler.handle(
                new CreatePromotionCommand(PRODUCT_ID, 15, TRANSLATIONS, START, END, true, true, 4));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("CAROUSEL_LIMIT_EXCEEDED");
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void should_allow_creation_when_carousel_is_below_limit() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(false);
        OfferCarouselSettings settings = OfferCarouselSettings.reconstitute(
                Map.of(), 5, Instant.now(), Instant.now());
        when(settingsRepository.find()).thenReturn(Optional.of(settings));
        when(promotionRepository.countVisibleInCarousel(null)).thenReturn(2L); // 2 < 5
        when(promotionRepository.existsCarouselOrder(any(), any())).thenReturn(false);

        Result<UUID> result = handler.handle(
                new CreatePromotionCommand(PRODUCT_ID, 10, TRANSLATIONS, START, END, true, true, 3));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void should_use_default_max_slides_when_no_settings_configured() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(false);
        when(settingsRepository.find()).thenReturn(Optional.empty()); // no settings → default = 5
        when(promotionRepository.countVisibleInCarousel(null))
                .thenReturn((long) OfferCarouselSettings.DEFAULT_MAX_SLIDES); // carousel full

        Result<UUID> result = handler.handle(
                new CreatePromotionCommand(PRODUCT_ID, 10, TRANSLATIONS, START, END, true, true, 1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("CAROUSEL_LIMIT_EXCEEDED");
    }

    @Test
    void should_fail_when_carousel_order_is_already_used() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(false);
        when(settingsRepository.find()).thenReturn(Optional.empty());
        when(promotionRepository.countVisibleInCarousel(null)).thenReturn(0L);
        when(promotionRepository.existsCarouselOrder(1, null)).thenReturn(true);

        Result<UUID> result = handler.handle(
                new CreatePromotionCommand(PRODUCT_ID, 10, TRANSLATIONS, START, END, true, true, 1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("CAROUSEL_ORDER_CONFLICT");
        verify(promotionRepository, never()).save(any());
    }
}
