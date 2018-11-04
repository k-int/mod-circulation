package org.folio.circulation.support.results;

import static api.support.matchers.FailureMatcher.isErrorFailureContaining;
import static org.folio.circulation.support.HttpResult.failed;
import static org.folio.circulation.support.HttpResult.succeeded;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.folio.circulation.support.HttpResult;
import org.folio.circulation.support.ServerErrorFailure;
import org.folio.circulation.support.WritableHttpResult;
import org.junit.Test;

public class HttpResultMappingTests {
  @Test
  public void shouldSucceedWhenMapIsApplied() {
    final HttpResult<Integer> mappedResult = succeeded(10)
      .map(value -> value + 10);

    assertThat(mappedResult.succeeded(), is(true));
    assertThat(mappedResult.value(), is(20));
  }

  @Test
  public void shouldFailWhenAlreadyFailed() {
    final HttpResult<Integer> result = failedResult()
      .map(value -> value + 10);

    assertThat(result, isErrorFailureContaining("Something went wrong"));
  }

  @Test
  public void shouldFailWhenExceptionThrownDuringMapping() {
    final HttpResult<Integer> result = succeeded(10)
      .map(value -> { throw exampleException(); });

    assertThat(result, isErrorFailureContaining("Something went wrong"));
  }

  private WritableHttpResult<Integer> failedResult() {
    return failed(new ServerErrorFailure(exampleException()));
  }

  private RuntimeException exampleException() {
    return new RuntimeException("Something went wrong");
  }
}