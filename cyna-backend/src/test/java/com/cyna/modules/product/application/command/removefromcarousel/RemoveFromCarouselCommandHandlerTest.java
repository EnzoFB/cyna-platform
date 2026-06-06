package com.cyna.modules.product.application.command.removefromcarousel;

import com.cyna.modules.product.domain.model.CarouselSlot;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.model.PromotionTranslation;
import com.cyna.modules.product.domain.repository.CarouselSlotRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
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
class RemoveFromCarouselCommandHandlerTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private CarouselSlotRepository carouselSlotRepository;

    private RemoveFromCarouselCommandHandler handler;

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
        handler = new RemoveFromCarouselCommandHandler(promotionRepository, carouselSlotRepository, transactionRunner);
    }

    @Test
    void should_remove_from_carousel_and_compact_remaining_slots() {
        UUID otherId = UUID.randomUUID();
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
        // After removal, one slot remains at order 2 → should be compacted to order 1
        when(carouselSlotRepository.findAll()).thenReturn(List.of(new CarouselSlot(otherId, 2)));

        Result<Void> result = handler.handle(new RemoveFromCarouselCommand(PROMOTION_ID));

        assertThat(result.isSuccess()).isTrue();
        verify(carouselSlotRepository).remove(PROMOTION_ID);
        verify(carouselSlotRepository).reorder(List.of(otherId));
    }

    @Test
    void should_not_call_reorder_when_carousel_is_empty_after_removal() {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
        when(carouselSlotRepository.findAll()).thenReturn(List.of());

        Result<Void> result = handler.handle(new RemoveFromCarouselCommand(PROMOTION_ID));

        assertThat(result.isSuccess()).isTrue();
        verify(carouselSlotRepository).remove(PROMOTION_ID);
        verify(carouselSlotRepository, never()).reorder(any());
    }

    @Test
    void should_fail_when_promotion_not_found() {
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.empty());

        Result<Void> result = handler.handle(new RemoveFromCarouselCommand(PROMOTION_ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("NOT_FOUND");
        verify(carouselSlotRepository, never()).remove(any());
    }
}
