package org.folio.circulation.support;

import io.vertx.core.json.JsonObject;
import org.folio.circulation.support.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import static org.folio.circulation.support.HttpResult.failed;

public class SingleRecordMapper<T> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Function<JsonObject, T> mapper;
  private final Function<Response, HttpResult<T>> resultOnFailure;

  public static <T> SingleRecordMapper<T> notFound(Function<JsonObject, T> mapper, HttpResult<T> notFoundResult) {
    return new SingleRecordMapper<>(mapper, notFoundMapper(notFoundResult));
  }

  SingleRecordMapper(Function<JsonObject, T> mapper) {
    this(mapper, (response -> HttpResult.failed(new ForwardOnFailure(response))));
  }

  public SingleRecordMapper(
    Function<JsonObject, T> mapper,
    Function<Response, HttpResult<T>> responseHttpResultFunction) {
    this.mapper = mapper;
    resultOnFailure = responseHttpResultFunction;
  }

  HttpResult<T> mapFrom(Response response) {
    if(response != null) {
      log.info("Response received, status code: {} body: {}",
        response.getStatusCode(), response.getBody());

      if (response.getStatusCode() == 200) {
        return HttpResult.succeeded(mapper.apply(response.getJson()));
      } else {
        return this.resultOnFailure.apply(response);
      }
    }
    else {
      log.warn("Did not receive response to request");
      return HttpResult.failed(new ServerErrorFailure("Did not receive response to request"));
    }
  }

  private static <T> Function<Response, HttpResult<T>> notFoundMapper(HttpResult<T> result) {
    return response -> response.getStatusCode() == 404
      ? result
      : failed(new ForwardOnFailure(response));
  }
}
