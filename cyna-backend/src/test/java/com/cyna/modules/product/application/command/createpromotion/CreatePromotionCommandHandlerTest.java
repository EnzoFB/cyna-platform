package com.cyna.modules.product.application.command.createpromotion;

import com.cyna.modules.product.domain.model.PromotionTranslation;
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
        handler = new CreatePromotionCommandHandler(productRepository, promotionRepository, transactionRunner);
    }

    @Test
    void should_create_promotion_successfully() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(false);

        Result<UUID> result = handler.handle(new CreatePromotionCommand(PRODUCT_ID, 20, TRANSLATIONS, START, END, true));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isNotNull();
        verify(promotionRepository).save(any());
    }

    @Test
    void should_fail_when_product_not_found() {
        UUID unknownId = UUID.randomUUID();
        when(productRepository.existsById(unknownId)).thenReturn(false);

        Result<UUID> result = handler.handle(
                new CreatePromotionCommand(unknownId, 10, TRANSLATIONS, START, END, true));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("PRODUCT_NOT_FOUND");
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void should_fail_when_enabled_promotion_overlaps() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(promotionRepository.existsEnabledOverlappingWindow(any(), any(), any(), any())).thenReturn(true);

        Result<UUID> result = handler.handle(
                new CreatePromotionCommand(PRODUCT_ID, 10, TRANSLATIONS, START, END, true));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("PROMOTION_OVERLAP");
        verify(promotionRepository, never()).save(any());
    }
}
