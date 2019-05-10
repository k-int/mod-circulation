package org.folio.circulation.support.http.client;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.folio.circulation.support.Result;
import org.folio.circulation.support.ServerErrorFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;

public class ResponseHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ResponseHandler() { }

  public static Handler<HttpClientResponse> any(CompletableFuture<Result<Response>> completed) {
    return responseConversationHandler(completed::complete);
  }

  public static Handler<HttpClientResponse> json(CompletableFuture<Result<Response>> completed) {
    return responseConversationHandler(
      responseResult -> completed.complete(responseResult.failWhen(
        ResponseHandler::isJson, ResponseHandler::expectedJsonError)));
  }

  private static ServerErrorFailure expectedJsonError(Response response) {
    return new ServerErrorFailure(
      String.format("Expected Json, actual: %s (Body: %s)",
        response.getContentType(), response.getBody()));
  }

  private static Result<Boolean> isJson(Response response) {
    return Result.of(() -> response.getContentType().contains("application/json"));
  }

  public static Handler<HttpClientResponse> responseConversationHandler(
    Consumer<Result<Response>> responseHandler) {

    return response -> response
      .bodyHandler(buffer -> responseHandler.accept(
        Result.of(() -> Response.from(response, buffer))))
      .exceptionHandler(ex -> {
        log.error("Unhandled exception in body handler", ex);
        responseHandler.accept(Result.failed(ex));
      });
  }
}
