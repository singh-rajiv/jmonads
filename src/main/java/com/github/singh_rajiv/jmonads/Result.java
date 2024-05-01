package com.github.singh_rajiv.jmonads;

import org.jetbrains.annotations.*;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Result<T> {
    private final Exception exception;
    private final T value;

    @NotNull
    @Contract("_ -> new")
    public static <T> Result<T> ofValue(T value) {
        Objects.requireNonNull(value);
        return new Result<>(null, value);
    }

    @Contract("_ -> new")
    public static <T> @NotNull Result<T> ofException(Exception exception) {
        Objects.requireNonNull(exception);
        return new Result<>(exception, null);
    }

    private Result(Exception exception, T value) {
        this.exception = exception;
        this.value = value;
    }

    public boolean isValue() {
        return value != null;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    public Optional<Exception> getException() {
        return Optional.ofNullable(exception);
    }

    public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        if (isValue()) {
            return ofValue(mapper.apply(value));
        } else {
            return ofException(exception);
        }
    }

    public <U> Result<U> flatMap(Function<? super T, Result<U>> mapper) {
        if (isValue()) {
            return mapper.apply(value);
        } else {
            return ofException(exception);
        }
    }

    public <U> Optional<U> match(Function<? super T, Optional<U>> whenValue, Function<? super Exception, Optional<U>> whenEx) {
        return isValue() ? whenValue.apply(value) :  whenEx.apply(exception);
    }

    public <U> CompletableFuture<Optional<U>> matchAsync(Function<? super T, Optional<U>> whenValue, Function<? super Exception, Optional<U>> whenEx) {
        return CompletableFuture.supplyAsync(() -> isValue() ? whenValue.apply(value) :  whenEx.apply(exception));
    }

    public T orElse(T other) {
        return value != null ? value : other;
    }

    public T orElseGet(Function<? super Exception, ? extends T> exceptionMapper) {
        return value != null ? value : exceptionMapper.apply(exception);
    }

    public T orElseThrow() throws Exception {
        if (exception != null) {
            throw exception;
        }
        return value;
    }

    public @NotNull CompletableFuture<T> toCompletableFuture() {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (value != null) {
            future.complete(value);
        } else {
            future.completeExceptionally(exception);
        }
        return future;
    }

    @Contract("_ -> new")
    public static <T> @NotNull Result<T> tryFunc(Supplier<T> f) {
        try {
            return new Result<>(null, f.get());
        } catch (Exception ex) {
            return new Result<>(ex, null);
        }
    }

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

    @Contract("_, _ -> new")
    public static <T1, T2> @NotNull Result<T2> tryFunc(Function<T1, T2> f, T1 val) {
        return tryFunc(() -> f.apply(val));
    }

    @Contract("_, _ -> new")
    public static <T1, T2> @NotNull CompletableFuture<Result<T2>> tryFuncAsync(Function<T1, T2> f, T1 val) {
        return tryFuncAsync(() -> f.apply(val));
    }
}

