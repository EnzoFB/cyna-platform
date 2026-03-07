package com.cyna.shared.application;

import java.util.function.Supplier;

/**
 * Runs a unit of work within a transaction boundary.
 * Keeps the application layer free of Spring's @Transactional annotation.
 *
 * Implementation is provided by SpringTransactionRunner in the infrastructure layer.
 */
public interface TransactionRunner {

    /**
     * Executes the given action within a transaction.
     * If the action throws an exception, the transaction is rolled back.
     */
    void run(Runnable action);

    /**
     * Executes the given supplier within a transaction and returns its result.
     * If the supplier throws an exception, the transaction is rolled back.
     */
    <T> T runReturning(Supplier<T> action);
}
