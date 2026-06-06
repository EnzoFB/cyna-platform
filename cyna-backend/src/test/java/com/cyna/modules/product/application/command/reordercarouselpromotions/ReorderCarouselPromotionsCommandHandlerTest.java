package com.cyna.modules.product.application.command.reordercarouselpromotions;

import com.cyna.modules.product.domain.model.CarouselSlot;
import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.repository.CarouselSlotRepository;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
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
class ReorderCarouselPromotionsCommandHandlerTest {

    @Mock private CarouselSlotRepository carouselSlotRepository;
    @Mock private OfferCarouselSettingsRepository settingsRepository;

    private ReorderCarouselPromotionsCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public void run(Runnable action) { action.run(); }
        @Override public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    private static final UUID ID_A = UUID.randomUUID();
    private static final UUID ID_B = UUID.randomUUID();
    private static final UUID ID_C = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ReorderCarouselPromotionsCommandHandler(
                carouselSlotRepository, settingsRepository, transactionRunner);
    }

    @Test
    void should_reorder_carousel_successfully() {
        when(settingsRepository.find()).thenReturn(Optional.empty()); // default maxSlides = 5
        when(carouselSlotRepository.findAll()).thenReturn(List.of(
                new CarouselSlot(ID_A, 1),
                new CarouselSlot(ID_B, 2),
                new CarouselSlot(ID_C, 3)
        ));

        Result<Void> result = handler.handle(new ReorderCarouselPromotionsCommand(List.of(ID_C, ID_A, ID_B)));

        assertThat(result.isSuccess()).isTrue();
        verify(carouselSlotRepository).reorder(List.of(ID_C, ID_A, ID_B));
    }

    @Test
    void should_fail_when_ordered_ids_is_null() {
        Result<Void> result = handler.handle(new ReorderCarouselPromotionsCommand(null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("VALIDATION_ERROR");
        verify(carouselSlotRepository, never()).reorder(any());
    }

    @Test
    void should_fail_when_ordered_ids_is_empty() {
        Result<Void> result = handler.handle(new ReorderCarouselPromotionsCommand(List.of()));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("VALIDATION_ERROR");
        verify(carouselSlotRepository, never()).reorder(any());
    }

    @Test
    void should_fail_when_size_exceeds_max_slides() {
        OfferCarouselSettings settings = OfferCarouselSettings.reconstitute(Map.of(), 2, Instant.now(), Instant.now());
        when(settingsRepository.find()).thenReturn(Optional.of(settings));

        Result<Void> result = handler.handle(
                new ReorderCarouselPromotionsCommand(List.of(ID_A, ID_B, ID_C)));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("CAROUSEL_LIMIT_EXCEEDED");
        verify(carouselSlotRepository, never()).reorder(any());
    }

    @Test
    void should_fail_when_ordered_ids_contains_duplicates() {
        when(settingsRepository.find()).thenReturn(Optional.empty());

        Result<Void> result = handler.handle(
                new ReorderCarouselPromotionsCommand(List.of(ID_A, ID_B, ID_A)));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("VALIDATION_ERROR");
        assertThat(result.getError()).contains("duplicate");
        verify(carouselSlotRepository, never()).reorder(any());
    }

    @Test
    void should_fail_when_id_is_not_in_carousel() {
        UUID unknownId = UUID.randomUUID();
        when(settingsRepository.find()).thenReturn(Optional.empty());
        when(carouselSlotRepository.findAll()).thenReturn(List.of(
                new CarouselSlot(ID_A, 1),
                new CarouselSlot(ID_B, 2)
        ));

        Result<Void> result = handler.handle(
                new ReorderCarouselPromotionsCommand(List.of(ID_A, unknownId)));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("NOT_FOUND");
        verify(carouselSlotRepository, never()).reorder(any());
    }
}
