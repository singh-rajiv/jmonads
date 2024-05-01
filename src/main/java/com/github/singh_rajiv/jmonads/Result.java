package com.github.singh_rajiv.jmonads;

import org.jetbrains.annotations.*;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a Result, which can contain either a value of type T or an exception.
 *
 * @param <T> the type of the value
 */
public final class Result<T> {
    private final Exception exception;
    private final T value;

    /**
     * Creates a Result with the given value.
     *
     * @param value the value (must not be null)
     * @param <T>   the type of the value
     * @return a Result containing the value
     * @throws NullPointerException if the value is null
     */
    @NotNull
    @Contract("_ -> new")
    public static <T> Result<T> ofValue(T value) {
        Objects.requireNonNull(value);
        return new Result<>(null, value);
    }

    /**
     * Creates a Result with the given exception.
     *
     * @param exception the exception (must not be null)
     * @param <T>       the type of the value
     * @return a Result containing the exception
     * @throws NullPointerException if the exception is null
     */
    @Contract("_ -> new")
    public static <T> @NotNull Result<T> ofException(Exception exception) {
        Objects.requireNonNull(exception);
        return new Result<>(exception, null);
    }

    /**
     * Constructs a Result with the given exception and value.
     *
     * @param exception the exception (can be null)
     * @param value     the value (can be null)
     */
    private Result(Exception exception, T value) {
        this.exception = exception;
        this.value = value;
    }

    /**
     * Checks if this Result contains a value.
     *
     * @return true if this Result contains a value, false otherwise
     */
    public boolean isValue() {
        return value != null;
    }

    /**
     * Returns an Optional containing the value, if present.
     *
     * @return an Optional containing the value, or an empty Optional if the value is null
     */
    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    /**
     * Returns an Optional containing the exception, if present.
     *
     * @return an Optional containing the exception, or an empty Optional if the exception is null
     */
    public Optional<Exception> getException() {
        return Optional.ofNullable(exception);
    }

    /**
     * Maps the value of this Result using the given mapper function.
     *
     * @param mapper the mapper function
     * @param <U>    the type of the mapped value
     * @return a Result containing the mapped value, or a Result containing the same exception if no value is present
     */
    public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        if (isValue()) {
            return ofValue(mapper.apply(value));
        } else {
            return ofException(exception);
        }
    }

    /**
     * Maps the value of this Result using the given mapper function, which returns a Result.
     *
     * @param mapper the mapper function
     * @param <U>    the type of the mapped value
     * @return a Result containing the mapped value, or a Result containing the same exception if no value is present
     */
    public <U> Result<U> flatMap(Function<? super T, Result<U>> mapper) {
        if (isValue()) {
            return mapper.apply(value);
        } else {
            return ofException(exception);
        }
    }

    /**
     * Matches the value of this Result using the given functions.
     *
     * @param whenValue the function to apply if a value is present
     * @param whenEx    the function to apply if an exception is present
     * @param <U>       the type of the result
     * @return the result of applying the matching function
     */
    public <U> Optional<U> match(Function<? super T, Optional<U>> whenValue, Function<? super Exception, Optional<U>> whenEx) {
        return isValue() ? whenValue.apply(value) :  whenEx.apply(exception);
    }

    /**
     * Asynchronously matches the value of this Result using the given functions.
     *
     * @param whenValue the function to apply if a value is present
     * @param whenEx    the function to apply if an exception is present
     * @param <U>       the type of the result
     * @return a CompletableFuture representing the result of applying the matching function
     */
    public <U> CompletableFuture<Optional<U>> matchAsync(Function<? super T, Optional<U>> whenValue, Function<? super Exception, Optional<U>> whenEx) {
        return CompletableFuture.supplyAsync(() -> isValue() ? whenValue.apply(value) :  whenEx.apply(exception));
    }

    /**
     * Returns the value of this Result if present, otherwise returns the specified default value.
     *
     * @param other the default value
     * @return the value if present, otherwise the default value
     */
    public T orElse(T other) {
        return value != null ? value : other;
    }

    /**
     * Returns the value of this Result if present, otherwise applies the specified function to the exception.
     *
     * @param exceptionMapper the function to apply to the exception
     * @return the value if present, otherwise the result of applying the function to the exception
     */
    public T orElseGet(Function<? super Exception, ? extends T> exceptionMapper) {
        return value != null ? value : exceptionMapper.apply(exception);
    }

    /**
     * Returns the value of this Result if present, otherwise throws the exception.
     *
     * @return the value if present
     * @throws Exception if no value is present
     */
    public T orElseThrow() throws Exception {
        if (exception != null) {
            throw exception;
        }
        return value;
    }

    /**
     * Converts this Result to a CompletableFuture.
     *
     * @return a CompletableFuture representing this Result
     */
    public @NotNull CompletableFuture<T> toCompletableFuture() {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (value != null) {
            future.complete(value);
        } else {
            future.completeExceptionally(exception);
        }
        return future;
    }

    /**
     * Calls the given function and wraps its returned value, returning a new Result.
     *
     * @param f   the function to apply
     * @param <U> the type of the new value
     * @return a new Result containing the result of applying the function
     */
    @Contract("_ -> new")
    public static <T> @NotNull Result<T> tryFunc(Supplier<T> f) {
        try {
            return new Result<>(null, f.get());
        } catch (Exception ex) {
            return new Result<>(ex, null);
        }
    }

    /**
     * Asynchronously calls the given function and wraps its returned value, returning a new Result.
     *
     * @param f   the function to apply
     * @param <U> the type of the new value
     * @return a CompletableFuture representing the new Result
     */
    @Contract("_ -> new")
    public static <T> @NotNull CompletableFuture<Result<T>> tryFuncAsync(Supplier<T> f) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new Result<>(null, f.get());
            } catch (Exception ex) {
                return new Result<>(ex, null);
            }
        });
    }

    /**
     * Applies the given function to the given value, returning a new Result.
     *
     * @param f   the function to apply
     * @param val the value to apply the function to
     * @param <T1> the type of the input value
     * @param <T2> the type of the new value
     * @return a new Result containing the result of applying the function
     */
    @Contract("_, _ -> new")
    public static <T1, T2> @NotNull Result<T2> tryFunc(Function<T1, T2> f, T1 val) {
        return tryFunc(() -> f.apply(val));
    }

    /**
     * Asynchronously applies the given function to the given value, returning a new Result.
     *
     * @param f   the function to apply
     * @param val the value to apply the function to
     * @param <T1> the type of the input value
     * @param <T2> the type of the new value
     * @return a CompletableFuture representing the new Result
     */
    @Contract("_, _ -> new")
    public static <T1, T2> @NotNull CompletableFuture<Result<T2>> tryFuncAsync(Function<T1, T2> f, T1 val) {
        return tryFuncAsync(() -> f.apply(val));
    }
}

