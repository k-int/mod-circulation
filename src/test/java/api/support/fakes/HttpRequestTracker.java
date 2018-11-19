package api.support.fakes;

import static java.util.Collections.synchronizedSet;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

class HttpRequestTracker {
  private static final Set<String> queries = synchronizedSet(new HashSet<>());

  Stream<String> getRequestStream() {
    return queries.stream();
  }

  void trackQuery(RoutingContext routingContext, String query, FakeStorageModule fakeStorageModule) {
    if(query != null) {
      queries.add(formattedRequest(routingContext.request()));
    }
  }

  private String formattedRequest(HttpServerRequest request) {
    return String.format("%s %s", request.method(), request.uri());
  }
}
