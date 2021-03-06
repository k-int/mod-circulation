package org.folio.circulation.resources;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.folio.circulation.loanrules.LoanRulesException;
import org.folio.circulation.loanrules.Text2Drools;
import org.folio.circulation.support.Clients;
import org.folio.circulation.support.CollectionResourceClient;
import org.folio.circulation.support.JsonHttpResult;
import org.folio.circulation.support.OkJsonHttpResult;
import org.folio.circulation.support.http.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.folio.circulation.support.http.server.ServerErrorResponse.internalError;

/**
 * Write and read the loan rules.
 */
public class LoanRulesResource extends Resource {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final String rootPath;

  /**
   * Set the URL path.
   * @param rootPath  URL path
   * @param client
   */
  public LoanRulesResource(String rootPath, HttpClient client) {
    super(client);
    this.rootPath = rootPath;
  }

  /**
   * Register the path set in the constructor.
   * @param router  where to register
   */
  public void register(Router router) {
    router.put(rootPath).handler(BodyHandler.create());

    router.get(rootPath).handler(this::get);
    router.put(rootPath).handler(this::put);
  }

  private void get(RoutingContext routingContext) {
    final Clients clients = Clients.create(new WebContext(routingContext), client);
    CollectionResourceClient loansRulesClient = clients.loanRulesStorage();

    log.debug("get(RoutingContext) client={}", loansRulesClient);

    if (loansRulesClient == null) {
      internalError(routingContext.response(),
        "Cannot initialise client to storage interface");
      return;
    }

    loansRulesClient.get().thenAccept(response -> {
      try {
        if (response.getStatusCode() != 200) {
          ForwardResponse.forward(routingContext.response(), response);
          return;
        }
        JsonObject loanRules = new JsonObject(response.getBody());
        loanRules.put("loanRulesAsDrools", Text2Drools.convert(
          loanRules.getString("loanRulesAsTextFile")));

        new OkJsonHttpResult(loanRules)
          .writeTo(routingContext.response());
      }
      catch (Exception e) {
        internalError(routingContext.response(), ExceptionUtils.getStackTrace(e));
      }
    });
  }

  //Cannot combine exception catching as cannot resolve overloaded method for error
  @SuppressWarnings("squid:S2147")
  private void put(RoutingContext routingContext) {
    final Clients clients = Clients.create(new WebContext(routingContext), client);
    CollectionResourceClient loansRulesClient = clients.loanRulesStorage();

    if (loansRulesClient == null) {
      internalError(routingContext.response(),
        "Cannot initialise client to storage interface");
      return;
    }

    JsonObject rulesInput;
    try {
      // try to convert, do not safe if conversion fails
      rulesInput = routingContext.getBodyAsJson();
      Text2Drools.convert(rulesInput.getString("loanRulesAsTextFile"));
    } catch (LoanRulesException e) {
      loanRulesError(routingContext.response(), e);
      return;
    } catch (DecodeException e) {
      loanRulesError(routingContext.response(), e);
      return;
    } catch (Exception e) {
      internalError(routingContext.response(), ExceptionUtils.getStackTrace(e));
      return;
    }
    LoanRulesEngineResource.clearCache(new WebContext(routingContext).getTenantId());
    JsonObject rules = rulesInput.copy();

    rules.remove("loanRulesAsDrools");

    loansRulesClient.put(rules).thenAccept(response -> {
      if (response.getStatusCode() == 204) {
        SuccessResponse.noContent(routingContext.response());
      } else {
        ForwardResponse.forward(routingContext.response(), response);
      }
    });
  }

  private static void loanRulesError(HttpServerResponse response, LoanRulesException e) {
    JsonObject body = new JsonObject();
    body.put("message", e.getMessage());
    body.put("line", e.getLine());
    body.put("column", e.getColumn());
    new JsonHttpResult(422, body, null).writeTo(response);
  }

  private static void loanRulesError(HttpServerResponse response, DecodeException e) {
    JsonObject body = new JsonObject();
    body.put("message", e.getMessage());  // already contains line and column number
    new JsonHttpResult(422, body, null).writeTo(response);
  }
}
