package org.folio.circulation.support;

import io.vertx.core.http.HttpServerResponse;
import org.folio.circulation.support.http.client.Response;
import org.folio.circulation.support.http.server.ForwardResponse;

public class ForwardOnFailure implements HttpFailure {
  private final Response failureResponse;

  public ForwardOnFailure(Response failureResponse) {
    this.failureResponse = failureResponse;
  }

  @Override
  public void writeTo(HttpServerResponse response) {
    ForwardResponse.forward(response, failureResponse);
  }
}
