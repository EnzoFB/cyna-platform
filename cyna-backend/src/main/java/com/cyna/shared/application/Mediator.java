package com.cyna.shared.application;

import com.cyna.shared.domain.Result;

/**
 * Central dispatcher for commands and queries.
 * Controllers inject only this interface — never individual handlers.
 */
public interface Mediator {

    /**
     * Dispatches a command to its handler.
     *
     * @param command the command to execute
     * @param <R>     the success result type
     * @return the result of the command execution
     */
    <R> Result<R> send(Command<R> command);

    /**
     * Dispatches a query to its handler.
     *
     * @param query the query to execute
     * @param <R>   the result type
     * @return the result of the query execution
     */
    <R> R send(Query<R> query);
}
