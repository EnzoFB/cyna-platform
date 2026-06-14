package com.cyna.modules.product.application.query.getoffercarouselsettings;

import com.cyna.modules.product.application.translation.CarouselSettingsTranslationDto;
import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class GetOfferCarouselSettingsQueryHandler implements QueryHandler<GetOfferCarouselSettingsQuery, OfferCarouselSettingsReadModel> {

    private final OfferCarouselSettingsRepository settingsRepository;

    public GetOfferCarouselSettingsQueryHandler(OfferCarouselSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    public OfferCarouselSettingsReadModel handle(GetOfferCarouselSettingsQuery query) {
        OfferCarouselSettings settings = settingsRepository.find()
                .orElseGet(OfferCarouselSettings::createDefault);
        return new OfferCarouselSettingsReadModel(
                CarouselSettingsTranslationDto.fromDomainMap(settings.getTranslations()),
                settings.getMaxSlides());
    }
}
