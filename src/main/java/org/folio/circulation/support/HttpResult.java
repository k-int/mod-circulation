package org.folio.circulation.support;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface HttpResult<T> {

  /**
   * Creates a successful result with the supplied value
   * unless an exception is thrown
   *
   * @param supplier of the result value
   * @return successful result or failed result with error
   */
  static <T> HttpResult<T> of(ThrowingSupplier<T, Exception> supplier) {
    try {
      return succeeded(supplier.get());
    } catch (Exception e) {
      return failed(e);
    }
  }

  /**
   * Creates a completed future with successful result with the supplied value
   * unless an exception is thrown
   *
   * @param supplier of the result value
   * @return completed future with successful result or failed result with error
   */
  static <T> CompletableFuture<HttpResult<T>> ofAsync(
    ThrowingSupplier<T, Exception> supplier) {

    return completedFuture(of(supplier));
  }

  /**
   * Combines two results together, if both succeed.
   * Otherwise, returns either failure, first result takes precedence
   *
   * Deprecated, please use member method version
   *
   * @param firstResult the  first result
   * @param secondResult the  second result
   * @param combiner function to combine the values together
   * @return either failure from the first result, failure from the second
   * or successful result with the values combined
   */
  //TODO: Replace with member method below
  static <T, U, V> HttpResult<V> combine(
    HttpResult<T> firstResult,
    HttpResult<U> secondResult,
    BiFunction<T, U, V> combiner) {

    return firstResult.combine(secondResult, combiner);
  }

  /**
   * Combines this and another result together, if both succeed.
   * Otherwise, returns either failure, this result takes precedence
   *
   * @param otherResult the other result
   * @param combiner function to combine the values together
   * @return either failure from this result, failure from the other
   * or successful result with the values combined
   */
  default <U, V> HttpResult<V> combine(
    HttpResult<U> otherResult,
    BiFunction<T, U, V> combiner) {

    return next(firstValue ->
      otherResult.map(secondValue ->
        combiner.apply(firstValue, secondValue)));
  }

  /**
   * Combines this and another result together, if both succeed.
   * Otherwise, returns either failure, this result takes precedence
   *
   * @param otherResult the other result
   * @param combiner function to combine the values together
   * @return either failure from this result, failure from the other
   * or result of the combination
   */
  default <U, V> HttpResult<V> combineToResult(
    HttpResult<U> otherResult,
    BiFunction<T, U, HttpResult<V>> combiner) {

    return next(firstValue ->
      otherResult.next(secondValue ->
        combiner.apply(firstValue, secondValue)));
  }

  /**
   * Combines a result together with the result of an action, if both succeed.
   * If the first result is a failure then it is returned, and the action is not invoked
   * otherwise if the result of the action is a failure it is returned
   *
   * @param nextAction the action to invoke if the current result succeeded
   * @param combiner function to combine the values together
   * @return either failure from the first result, failure from the action
   * or successful result with the values combined
   */
  default <U, V> CompletableFuture<HttpResult<V>> combineAfter(
    Function<T, CompletableFuture<HttpResult<U>>> nextAction,
    BiFunction<T, U, V> combiner) {

    return after(nextAction)
      .thenApply(actionResult -> combine(actionResult, combiner));
  }

  /**
   * Allows branching between two paths based upon the outcome of a condition
   *
   * Executes the whenTrue function when condition evaluates to true
   * Executes the whenFalse function when condition evaluates to false
   * Executes neither if the condition evaluation fails
   * Forwards on failure if previous result failed
   *
   * @param conditionFunction on which to branch upon
   * @param whenTrue executed when condition evaluates to true
   * @param whenFalse executed when condition evaluates to false
   * @return Result of whenTrue or whenFalse, unless previous result failed
   */
  default CompletableFuture<HttpResult<T>> afterWhen(
    Function<T, CompletableFuture<HttpResult<Boolean>>> conditionFunction,
    Function<T, CompletableFuture<HttpResult<T>>> whenTrue,
    Function<T, CompletableFuture<HttpResult<T>>> whenFalse) {

    return after(value ->
      conditionFunction.apply(value)
        .thenComposeAsync(r -> r.after(condition -> condition
          ? whenTrue.apply(value)
          : whenFalse.apply(value))));
  }

  /**
   * Fail a result when a condition evaluates to true
   *
   * Responds with the result of the failure function when condition evaluates to true
   * Responds with success of the prior result when condition evaluates to false
   * Executes neither if the condition evaluation fails
   * Forwards on failure if previous result failed
   *
   * @param condition on which to decide upon
   * @param failure executed to create failure reason when condition evaluates to true
   * @return success when condition is false, failure otherwise
   */
  default CompletableFuture<HttpResult<T>> failAfter(
    Function<T, CompletableFuture<HttpResult<Boolean>>> condition,
    Function<T, HttpFailure> failure) {

    return afterWhen(condition,
      value -> completedFuture(failed(failure.apply(value))),
      value -> completedFuture(succeeded(value)));
  }

  /**
   * Allows branching between two paths based upon the outcome of a condition
   *
   * Executes the whenTrue function when condition evaluates to true
   * Executes the whenFalse function when condition evaluates to false
   * Executes neither if the condition evaluation fails
   * Forwards on failure if previous result failed
   *
   * @param condition on which to branch upon
   * @param whenTrue executed when condition evaluates to true
   * @param whenFalse executed when condition evaluates to false
   * @return Result of whenTrue or whenFalse, unless previous result failed
   */
  default HttpResult<T> nextWhen(
    Function<T, HttpResult<Boolean>> condition,
    Function<T, HttpResult<T>> whenTrue,
    Function<T, HttpResult<T>> whenFalse) {

    return next(value ->
      when(condition.apply(value),
        () -> whenTrue.apply(value),
        () -> whenFalse.apply(value)));
  }

  /**
   * Allows branching between two paths based upon the outcome of a condition
   *
   * Executes the whenTrue function when condition evaluates to true
   * Executes the whenFalse function when condition evaluates to false
   * Executes neither if the condition evaluation fails
   * Forwards on failure if previous result failed
   *
   * @param condition on which to branch upon
   * @param whenTrue executed when condition evaluates to true
   * @param whenFalse executed when condition evaluates to false
   * @return Result of whenTrue or whenFalse, unless previous result failed
   */
  static <R> HttpResult<R> when(
    HttpResult<Boolean> condition,
    Supplier<HttpResult<R>> whenTrue,
    Supplier<HttpResult<R>> whenFalse) {

    return condition.next(result -> result
      ? whenTrue.get()
      : whenFalse.get());
  }

  /**
   * Fail a result when a condition evaluates to true
   *
   * Responds with the result of the failure function when condition evaluates to true
   * Responds with success of the prior result when condition evaluates to false
   * Executes neither if the condition evaluation fails
   * Forwards on failure if previous result failed
   *
   * @param condition on which to decide upon
   * @param failure executed to create failure reason when condition evaluates to true
   * @return success when condition is false, failure otherwise
   */
  default HttpResult<T> failWhen(
    Function<T, HttpResult<Boolean>> condition,
    Function<T, HttpFailure> failure) {

    return nextWhen(condition,
      value -> failed(failure.apply(value)),
      HttpResult::succeeded);
  }

  boolean failed();

  T value();
  HttpFailure cause();

  default boolean succeeded() {
    return !failed();
  }

  static <T> HttpResult<T> succeeded(T value) {
    return new SuccessfulHttpResult<>(value);
  }

  static <T> WritableHttpResult<T> failed(HttpFailure cause) {
    return new FailedHttpResult<>(cause);
  }

  static <T> HttpResult<T> failed(Throwable e) {
    return failed(new ServerErrorFailure(e));
  }

  default <R> CompletableFuture<HttpResult<R>> after(
    Function<T, CompletableFuture<HttpResult<R>>> action) {

    if(failed()) {
      return completedFuture(failed(cause()));
    }

    try {
      return action.apply(value())
        .exceptionally(HttpResult::failed);
    } catch (Exception e) {
      return completedFuture(failed(new ServerErrorFailure(e)));
    }
  }

  /**
   * Apply the next action to the value of the result
   *
   * Responds with the result of applying the next action to the current value
   * unless current result is failed or the application of action fails e.g. throws an exception
   *
   * @param action action to take after this result
   * @return success when result succeeded and action is applied successfully, failure otherwise
   */
  default <R> HttpResult<R> next(Function<T, HttpResult<R>> action) {
    if(failed()) {
      return failed(cause());
    }

    try {
      return action.apply(value());
    } catch (Exception e) {
      return failed(e);
    }
  }

  /**
   * Map the value of a result to a new value
   *
   * Responds with a new result with the outcome of applying the map to the current value
   * unless current result is failed or the mapping fails e.g. throws an exception
   *
   * @param map function to apply to value of result
   * @return success when result succeeded and map is applied successfully, failure otherwise
   */
  default <U> HttpResult<U> map(Function<T, U> map) {
    return next(value -> succeeded(map.apply(value)));
  }

  default T orElse(T other) {
    return succeeded()
      ? value()
      : other;
  }
}
