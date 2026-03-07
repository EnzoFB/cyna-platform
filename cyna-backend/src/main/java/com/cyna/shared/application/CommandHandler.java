package com.cyna.shared.application;

import com.cyna.shared.domain.Result;

/**
 * Handles a command and returns a Result.
 *
 * @param <C> the command type
 * @param <R> the success value type
 */
public interface CommandHandler<C extends Command<R>, R> {
    Result<R> handle(C command);
}
