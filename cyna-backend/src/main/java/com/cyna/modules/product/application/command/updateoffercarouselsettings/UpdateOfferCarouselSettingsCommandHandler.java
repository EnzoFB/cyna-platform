package com.cyna.modules.product.application.command.updateoffercarouselsettings;

import com.cyna.modules.product.application.translation.CarouselSettingsTranslationDto;
import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class UpdateOfferCarouselSettingsCommandHandler implements CommandHandler<UpdateOfferCarouselSettingsCommand, Void> {

    private final OfferCarouselSettingsRepository settingsRepository;
    private final TransactionRunner transactionRunner;

    public UpdateOfferCarouselSettingsCommandHandler(OfferCarouselSettingsRepository settingsRepository,
                                                     TransactionRunner transactionRunner) {
        this.settingsRepository = settingsRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(UpdateOfferCarouselSettingsCommand command) {
        return transactionRunner.runReturning(() -> {
            OfferCarouselSettings current = settingsRepository.find()
                    .orElseGet(OfferCarouselSettings::createDefault);
            settingsRepository.save(current.update(
                    CarouselSettingsTranslationDto.toDomainMap(command.translations()),
                    command.maxSlides()));
            return Result.success(null);
        });
    }
}
