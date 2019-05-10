package org.folio.circulation.support.http.client;

import static org.apache.http.entity.ContentType.TEXT_PLAIN;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;

public class ResponseHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ResponseHandler() { }

  public static Handler<HttpClientResponse> any(CompletableFuture<Response> completed) {
    return responseConversationHandler(completed::complete);
  }

  public static Handler<HttpClientResponse> json(CompletableFuture<Response> completed) {
    return responseHandler(completed,
      ResponseHandler::isJson, ResponseHandler::expectedJsonException);
  }

  private static Exception expectedJsonException(Response response) {
    return new Exception(
      String.format("Expected Json, actual: %s (Body: %s)",
        response.getContentType(), response.getBody()));
  }

  private static boolean isJson(Response response) {
    return response.getContentType().contains("application/json");
  }

  private static Handler<HttpClientResponse> responseHandler(
    CompletableFuture<Response> completed,
    Predicate<Response> expectation,
    Function<Response, Throwable> expectationFailed) {

    return responseConversationHandler(response -> {
      try {
        log.debug("Received Response: {}: {}", response.getStatusCode(), response.getContentType());
        log.debug("Received Response Body: {}", response.getBody());

        if(expectation.test(response)) {
          completed.complete(response);
        }
        else {
          completed.completeExceptionally(expectationFailed.apply(response));
        }
      } catch (Exception e) {
        completed.completeExceptionally(e);
      }
    });
  }

  public static Handler<HttpClientResponse> responseConversationHandler(
    Consumer<Response> responseHandler) {

    return response -> response
      .bodyHandler(buffer -> responseHandler.accept(Response.from(response, buffer)))
      .exceptionHandler(ex -> {
        log.error("Unhandled exception in body handler", ex);
        String trace = ExceptionUtils.getStackTrace(ex);
        responseHandler.accept(new Response(500, trace, TEXT_PLAIN.toString()));
      });
  }
}
