package com.cyna.modules.payment.application.command.consentlog;

import com.cyna.modules.payment.domain.model.ConsentAction;
import com.cyna.modules.payment.domain.model.PaymentConsentLog;
import com.cyna.modules.payment.domain.repository.PaymentConsentLogRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class LogPaymentConsentCommandHandler
        implements CommandHandler<LogPaymentConsentCommand, Void> {

    private final PaymentConsentLogRepository repository;
    private final TransactionRunner transactionRunner;

    public LogPaymentConsentCommandHandler(PaymentConsentLogRepository repository,
                                           TransactionRunner transactionRunner) {
        this.repository = repository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(LogPaymentConsentCommand command) {
        transactionRunner.run(() -> repository.save(PaymentConsentLog.record(
                command.userId(),
                ConsentAction.SAVE_CARD_AT_CHECKOUT,
                command.labelVersion(),
                command.stripePaymentMethodId(),
                command.ipAddress(),
                command.userAgent()
        )));
        return Result.success();
    }
}
