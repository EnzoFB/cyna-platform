package com.cyna.modules.payment.application.command.savedpaymentmethod;

import com.cyna.modules.payment.domain.repository.SavedPaymentMethodRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SetDefaultPaymentMethodCommandHandler
        implements CommandHandler<SetDefaultPaymentMethodCommand, Void> {

    private final SavedPaymentMethodRepository repository;

    public SetDefaultPaymentMethodCommandHandler(SavedPaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Result<Void> handle(SetDefaultPaymentMethodCommand command) {
        var method = repository.findByIdAndUserId(command.paymentMethodId(), command.userId())
                .orElse(null);

        if (method == null) {
            return Result.failure("NOT_FOUND");
        }

        repository.clearDefaultForUser(command.userId());
        repository.save(method.withDefault(true));
        return Result.success();
    }
}
