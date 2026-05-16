package com.cyna.modules.payment.application.command.consentlog;

import com.cyna.modules.payment.domain.model.PaymentConsentLog;
import com.cyna.modules.payment.domain.repository.PaymentConsentLogRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LogPaymentConsentCommandHandler
        implements CommandHandler<LogPaymentConsentCommand, Void> {

    private final PaymentConsentLogRepository repository;

    public LogPaymentConsentCommandHandler(PaymentConsentLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Result<Void> handle(LogPaymentConsentCommand command) {
        repository.save(PaymentConsentLog.record(
                command.userId(),
                command.action(),
                command.labelVersion(),
                command.stripePaymentMethodId(),
                command.ipAddress(),
                command.userAgent()
        ));
        return Result.success();
    }
}
