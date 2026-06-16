package com.cyna.modules.payment.application.api;

import com.cyna.modules.payment.domain.repository.PaymentConsentLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class PaymentQueryApiImpl implements PaymentQueryApi {

    private final PaymentConsentLogRepository consentLogRepository;

    PaymentQueryApiImpl(PaymentConsentLogRepository consentLogRepository) {
        this.consentLogRepository = consentLogRepository;
    }

    @Override
    public List<PaymentConsentExportView> exportConsentsForUser(UUID userId) {
        return consentLogRepository.findAllByUserId(userId).stream()
                .map(c -> new PaymentConsentExportView(
                        c.getAction().name(),
                        c.getLabelVersion(),
                        c.getStripePaymentMethodId(),
                        c.getIpAddress(),
                        c.getUserAgent(),
                        c.getGivenAt()))
                .toList();
    }
}
