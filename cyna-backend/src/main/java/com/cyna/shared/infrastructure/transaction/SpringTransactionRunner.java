package com.cyna.shared.infrastructure.transaction;

import com.cyna.shared.application.TransactionRunner;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Spring-based TransactionRunner implementation.
 * Uses TransactionTemplate for programmatic transaction management.
 */
@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    @Override
    public <T> T runReturning(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}
