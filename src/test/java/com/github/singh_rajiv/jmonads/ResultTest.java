package com.github.singh_rajiv.jmonads;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class ResultTest {

    @Test
    void ofValue_ShouldCreateResultWithValue() {
        var result = Result.ofValue(10);
        assertTrue(result.isValue());
        assertEquals(10, result.getValue().get());
    }

    @Test
    void ofException_ShouldCreateResultWithException() {
        var exception = new RuntimeException("Test Exception");
        var result = Result.ofException(exception);
        assertFalse(result.isValue());
        assertEquals(exception, result.getException().get());
    }

    @Test
    void map_ShouldApplyMapperToValue() {
        var result = Result.ofValue(10);
        Function<Integer, String> mapper = Object::toString;
        var mappedResult = result.map(mapper);
        assertTrue(mappedResult.isValue());
        assertEquals("10", mappedResult.getValue().get());
    }

    @Test
    void map_ShouldReturnExceptionResultIfExceptionOccurred() {
        var exception = new RuntimeException("Test Exception");
        Result<Integer> result = Result.ofException(exception);
        Function<Integer, String> mapper = Object::toString;
        var mappedResult = result.map(mapper);
        assertFalse(mappedResult.isValue());
        assertEquals(exception, mappedResult.getException().get());
    }

    @Test
    void flatMap_ShouldApplyMapperToValue() {
        var result = Result.ofValue(10);
        Function<Integer, Result<String>> mapper = val -> Result.ofValue(val.toString());
        var mappedResult = result.flatMap(mapper);
        assertTrue(mappedResult.isValue());
        assertEquals("10", mappedResult.getValue().get());
    }

    @Test
    void flatMap_ShouldReturnExceptionResultIfExceptionOccurred() {
        var exception = new RuntimeException("Test Exception");
        Result<Integer> result = Result.ofException(exception);
        Function<Integer, Result<String>> mapper = val -> Result.ofValue(val.toString());
        var mappedResult = result.flatMap(mapper);
        assertFalse(mappedResult.isValue());
        assertEquals(exception, mappedResult.getException().get());
    }

    @Test
    void match_ShouldApplyFunctionToValue() {
        var result = Result.ofValue(10);
        Function<Integer, Optional<String>> valueMapper = val -> Optional.of(val.toString());
        Function<Exception, Optional<String>> exceptionMapper = ex -> Optional.empty();
        var matchedResult = result.match(valueMapper, exceptionMapper);
        assertTrue(matchedResult.isPresent());
        assertEquals("10", matchedResult.get());
    }

    @Test
    void match_ShouldApplyFunctionToException() {
        var exception = new RuntimeException("Test Exception");
        Result<Integer> result = Result.ofException(exception);
        Function<Integer, Optional<String>> valueMapper = val -> Optional.of(val.toString());
        Function<Exception, Optional<String>> exceptionMapper = ex -> Optional.of(ex.getMessage());
        var matchedResult = result.match(valueMapper, exceptionMapper);
        assertTrue(matchedResult.isPresent());
        assertEquals("Test Exception", matchedResult.get());
    }

    @Test
    void matchAsync_ShouldApplyFunctionToValue() throws InterruptedException, ExecutionException, TimeoutException {
        var result = Result.ofValue(10);
        Function<Integer, Optional<String>> valueMapper = val -> Optional.of(val.toString());
        Function<Exception, Optional<String>> exceptionMapper = ex -> Optional.empty();
        var future = result.matchAsync(valueMapper, exceptionMapper);
        var matchedResult = future.get(1, TimeUnit.SECONDS);
        assertTrue(matchedResult.isPresent());
        assertEquals("10", matchedResult.get());
    }

    @Test
    void matchAsync_ShouldApplyFunctionToException() throws InterruptedException, ExecutionException, TimeoutException {
        var exception = new RuntimeException("Test Exception");
        Result<Integer> result = Result.ofException(exception);
        Function<Integer, Optional<String>> valueMapper = val -> Optional.of(val.toString());
        Function<Exception, Optional<String>> exceptionMapper = ex -> Optional.of(ex.getMessage());
        var future = result.matchAsync(valueMapper, exceptionMapper);
        var matchedResult = future.get(1, TimeUnit.SECONDS);
        assertTrue(matchedResult.isPresent());
        assertEquals("Test Exception", matchedResult.get());
    }

    @Test
    void orElse_ShouldReturnValueIfValueExists() {
        var result = Result.ofValue(10);
        assertEquals(10, result.orElse(5));
    }

    @Test
    void orElse_ShouldReturnOtherIfValueDoesNotExist() {
        var result = Result.ofException(new RuntimeException());
        assertEquals(5, result.orElse(5));
    }

    @Test
    void orElseGet_ShouldReturnValueIfValueExists() {
        var result = Result.ofValue(10);
        assertEquals(10, result.orElseGet(ex -> 5));
    }

    @Test
    void orElseGet_ShouldReturnOtherIfValueDoesNotExist() {
        var result = Result.ofException(new RuntimeException());
        assertEquals(5, result.orElseGet(ex -> 5));
    }

    @Test
    void orElseThrow_ShouldReturnValueIfValueExists() throws Exception {
        var result = Result.ofValue(10);
        assertEquals(10, result.orElseThrow());
    }

    @Test
    void orElseThrow_ShouldThrowExceptionIfValueDoesNotExist() {
        var result = Result.ofException(new RuntimeException());
        assertThrows(RuntimeException.class, result::orElseThrow);
    }

    @Test
    void toCompletableFuture_ShouldCompleteWithResultValue() throws InterruptedException, ExecutionException, TimeoutException {
        var result = Result.ofValue(10);
        var future = result.toCompletableFuture();
        assertEquals(10, future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void toCompletableFuture_ShouldCompleteExceptionallyIfExceptionOccurs()  {
        var exception = new RuntimeException("Test Exception");
        Result<Integer> result = Result.ofException(exception);
        var future = result.toCompletableFuture();
        assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void tryFunc_ShouldReturnResultWithValueIfFunctionSucceeds() {
        Supplier<Integer> supplier = () -> 10;
        var result = Result.tryFunc(supplier);
        assertTrue(result.isValue());
        assertEquals(10, result.getValue().get());
    }

    @Test
    void tryFunc_ShouldReturnResultWithExceptionIfFunctionThrowsException() {
        Supplier<Integer> supplier = () -> { throw new RuntimeException("Test Exception"); };
        var result = Result.tryFunc(supplier);
        assertFalse(result.isValue());
        assertTrue(result.getException().isPresent());
    }

    @Test
    void tryFuncAsync_ShouldReturnCompletedFutureWithValueIfFunctionSucceeds() throws InterruptedException, ExecutionException, TimeoutException {
        Supplier<Integer> supplier = () -> 10;
        var future = Result.tryFuncAsync(supplier);
        var result = future.get(1, TimeUnit.SECONDS);
        assertTrue(result.isValue());
        assertEquals(10, result.getValue().get());
    }

    @Test
    void tryFuncAsync_ShouldReturnCompletedFutureWithExceptionIfFunctionThrowsException() throws InterruptedException, ExecutionException, TimeoutException {
        Supplier<Integer> supplier = () -> { throw new RuntimeException("Test Exception"); };
        var future = Result.tryFuncAsync(supplier);
        var result = future.get(1, TimeUnit.SECONDS);
        assertFalse(result.isValue());
        assertTrue(result.getException().isPresent());
    }

    @Test
    void tryFunc_ShouldReturnResultWithValueIfFunctionWithParameterSucceeds() {
        Function<Integer, String> function = Object::toString;
        var result = Result.tryFunc(function, 10);
        assertTrue(result.isValue());
        assertEquals("10", result.getValue().get());
    }

    @Test
    void tryFunc_ShouldReturnResultWithExceptionIfFunctionWithParameterThrowsException() {
        Function<Integer, String> function = val -> { throw new RuntimeException("Test Exception"); };
        var result = Result.tryFunc(function, 10);
        assertFalse(result.isValue());
        assertTrue(result.getException().isPresent());
    }

    @Test
    void tryFuncAsync_ShouldReturnCompletedFutureWithValueIfFunctionWithParameterSucceeds() throws InterruptedException, ExecutionException, TimeoutException {
        Function<Integer, String> function = Object::toString;
        var future = Result.tryFuncAsync(function, 10);
        var result = future.get(1, TimeUnit.SECONDS);
        assertTrue(result.isValue());
        assertEquals("10", result.getValue().get());
    }

    @Test
    void tryFuncAsync_ShouldReturnCompletedFutureWithExceptionIfFunctionWithParameterThrowsException() throws InterruptedException, ExecutionException, TimeoutException {
        Function<Integer, String> function = val -> { throw new RuntimeException("Test Exception"); };
        var future = Result.tryFuncAsync(function, 10);
        var result = future.get(1, TimeUnit.SECONDS);
        assertFalse(result.isValue());
        assertTrue(result.getException().isPresent());
    }
}