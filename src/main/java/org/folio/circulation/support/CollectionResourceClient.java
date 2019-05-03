package org.folio.circulation.support;

import static org.folio.circulation.support.http.client.ResponseHandler.responseConversationHandler;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.folio.circulation.support.http.client.OkapiHttpClient;
import org.folio.circulation.support.http.client.Response;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;

public class CollectionResourceClient {
  private final OkapiHttpClient client;
  private final URL collectionRoot;

  public CollectionResourceClient(OkapiHttpClient client, URL collectionRoot) {
    this.client = client;
    this.collectionRoot = collectionRoot;
  }

  public CompletableFuture<Response> post(JsonObject resourceRepresentation) {
    return bindResponseToFuture(handler ->
      client.post(collectionRoot, resourceRepresentation, handler));
  }

  public CompletableFuture<Response> put(JsonObject resourceRepresentation) {
    return bindResponseToFuture(handler ->
      client.put(collectionRoot, resourceRepresentation, handler));
  }

  public CompletableFuture<Response> put(String id, JsonObject resourceRepresentation) {
    return bindResponseToFuture(handler ->
      client.put(individualRecordUrl(id), resourceRepresentation, handler));
  }

  public CompletableFuture<Response> get() {
    return bindResponseToFuture(handler -> client.get(collectionRoot, handler));
  }

  public CompletableFuture<Response> get(String id) {
    return bindResponseToFuture(handler ->
      client.get(individualRecordUrl(id), handler));
  }

  public CompletableFuture<Response> delete(String id) {
    return bindResponseToFuture(handler ->
      client.delete(individualRecordUrl(id), handler));
  }

  public CompletableFuture<Response> delete() {
    return bindResponseToFuture(handler -> client.delete(collectionRoot, handler));
  }

  /**
   * Make a get request for multiple records using raw query string parameters
   * Should only be used when passing on entire query string from a client request
   * Is deprecated, and will be removed when all use replaced by explicit parsing
   * of request parameters
   *
   * @param rawQueryString raw query string to append to the URL
   * @return response from the server
   */
  public CompletableFuture<Response> getManyWithRawQueryStringParameters(
    String rawQueryString) {

    String url = isProvided(rawQueryString)
      ? String.format("%s?%s", collectionRoot, rawQueryString)
      : collectionRoot.toString();

    return bindResponseToFuture(handler -> client.get(url, handler));
  }

  public CompletableFuture<Result<Response>> getMany(
    CqlQuery cqlQuery, Integer pageLimit) {

    return cqlQuery.encode()
      .map(query -> collectionRoot + createQueryString(query, pageLimit, 0))
      .after(url -> bindResponseToResult(handler -> client.get(url, handler)));
  }

  private static boolean isProvided(String query) {
    return StringUtils.isNotBlank(query);
  }

  /**
   * Combine the optional parameters to a query string.
   * <p>
   * createQueryString("field%3Da", 5, 10) = "?query=field%3Da&limit=5&offset=10"
   * <p>
   * createQueryString(null, 5, null) = "?limit=5"
   * <p>
   * createQueryString(null, null, null) = ""
   *
   * @param urlEncodedCqlQuery  the URL encoded String for the query parameter, may be null or empty for none
   * @param pageLimit  the value for the limit parameter, may be null for none
   * @param pageOffset  the value for the offset parameter, may be null for none
   * @return the query string, may be empty
   */
  static String createQueryString(
    String urlEncodedCqlQuery,
    Integer pageLimit,
    Integer pageOffset) {

    String query = "";

    if (isProvided(urlEncodedCqlQuery)) {
      query += "?query=" + urlEncodedCqlQuery;
    }
    if (pageLimit != null) {
      query += query.isEmpty() ? "?" : "&";
      query += "limit=" + pageLimit;
    }
    if (pageOffset != null) {
      query += query.isEmpty() ? "?" : "&";
      query += "offset=" + pageOffset;
    }
    return query;
  }
  
  private CompletableFuture<Result<Response>> bindResponseToResult(
    Consumer<Handler<HttpClientResponse>> requestInvoker) {

    return bindResponseToFuture(requestInvoker)
      .thenApply(Result::succeeded);
  }

  private CompletableFuture<Response> bindResponseToFuture(
    Consumer<Handler<HttpClientResponse>> requestInvoker) {

    final CompletableFuture<Response> future = new CompletableFuture<>();

    requestInvoker.accept(responseConversationHandler(future::complete));

    return future;
  }

  private String individualRecordUrl(String id) {
    return String.format("%s/%s", collectionRoot, id);
  }
}
