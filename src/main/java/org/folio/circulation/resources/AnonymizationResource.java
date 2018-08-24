package org.folio.circulation.resources;

import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.folio.circulation.support.OkJsonHttpResult;

public class AnonymizationResource extends Resource {
  public AnonymizationResource(HttpClient client) {
    super(client);
  }

  @Override
  public void register(Router router) {
    //TODO: Replace with route registration, for failure handling
    router.post("/circulation/loans/anonymize/:userId").handler(this::anonymize);
  }

  private void anonymize(RoutingContext routingContext) {
    String userId = routingContext.request().getParam("userId");

    new OkJsonHttpResult(new JsonObject().put("userId", userId))
      .writeTo(routingContext.response());
  }
}
