package com.cyna.shared.domain;

import java.util.function.Function;

/**
 * Result monad for explicit, composable error handling.
 * Replaces exceptions for business-level errors.
 *
 * @param <T> the type of the success value
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    boolean isSuccess();
    boolean isFailure();
    T getValue();
    String getError();

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static Result<Void> success() {
        return new Success<>(null);
    }

    static <T> Result<T> failure(String error) {
        return new Failure<>(error);
    }

    <R> R fold(Function<T, R> onSuccess, Function<String, R> onFailure);

    <R> Result<R> map(Function<T, R> mapper);

    <R> Result<R> flatMap(Function<T, Result<R>> mapper);

    // --- Implementations ---

    record Success<T>(T value) implements Result<T> {
        @Override public boolean isSuccess() { return true; }
        @Override public boolean isFailure() { return false; }
        @Override public T getValue() { return value; }
        @Override public String getError() { throw new UnsupportedOperationException("No error on success"); }

        @Override
        public <R> R fold(Function<T, R> onSuccess, Function<String, R> onFailure) {
            return onSuccess.apply(value);
        }

        @Override
        public <R> Result<R> map(Function<T, R> mapper) {
            return new Success<>(mapper.apply(value));
        }

        @Override
        public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
            return mapper.apply(value);
        }
    }

    record Failure<T>(String error) implements Result<T> {
        @Override public boolean isSuccess() { return false; }
        @Override public boolean isFailure() { return true; }
        @Override public T getValue() { throw new UnsupportedOperationException("No value on failure"); }
        @Override public String getError() { return error; }

        @Override
        public <R> R fold(Function<T, R> onSuccess, Function<String, R> onFailure) {
            return onFailure.apply(error);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R> map(Function<T, R> mapper) {
            return (Result<R>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
            return (Result<R>) this;
        }
    }
}
