package com.cyna.shared.application;

/**
 * Handles a query and returns the result directly.
 * Queries never modify state.
 *
 * @param <Q> the query type
 * @param <R> the result type
 */
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
