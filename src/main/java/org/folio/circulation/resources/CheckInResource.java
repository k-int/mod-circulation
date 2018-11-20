package org.folio.circulation.resources;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.LoanAndRelatedRecords;
import org.folio.circulation.domain.LoanRepository;
import org.folio.circulation.domain.LoanRepresentation;
import org.folio.circulation.domain.OpenLoanFinder;
import org.folio.circulation.domain.RequestQueue;
import org.folio.circulation.domain.RequestQueueRepository;
import org.folio.circulation.domain.UpdateItem;
import org.folio.circulation.domain.UpdateRequestQueue;
import org.folio.circulation.domain.UserRepository;
import org.folio.circulation.domain.representations.CheckInByBarcodeRequest;
import org.folio.circulation.domain.representations.LoanResponse;
import org.folio.circulation.support.Clients;
import org.folio.circulation.support.HttpResult;
import org.folio.circulation.support.ItemRepository;
import org.folio.circulation.support.RouteRegistration;
import org.folio.circulation.support.http.server.WebContext;

import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public abstract class CheckInResource extends Resource {
  private final String rootPath;

  CheckInResource(HttpClient client, String rootPath) {
    super(client);
    this.rootPath = rootPath;
  }

  @Override
  public void register(Router router) {
    RouteRegistration routeRegistration = new RouteRegistration(
      rootPath, router);

    routeRegistration.create(this::checkin);
  }

  private void checkin(RoutingContext routingContext) {

    final WebContext context = new WebContext(routingContext);

    final Clients clients = Clients.create(context, client);

    final LoanRepository loanRepository = new LoanRepository(clients);
    final UserRepository userRepository = new UserRepository(clients);
    final ItemRepository itemRepository = new ItemRepository(clients, false, false);
    final RequestQueueRepository requestQueueRepository = RequestQueueRepository.using(clients);

    final OpenLoanFinder openLoanFinder
      = new OpenLoanFinder(loanRepository, itemRepository, userRepository);

    final LoanRepresentation loanRepresentation = new LoanRepresentation();
//    final LoanCheckinService loanCheckinService = LoanCheckinService.using(clients);

    final UpdateRequestQueue requestQueueUpdate = UpdateRequestQueue.using(clients);
    final UpdateItem updateItem = new UpdateItem(clients);

    // TODO: Validation check for same user should be in the domain service

    final HttpResult<CheckInByBarcodeRequest> requestResult =
      CheckInByBarcodeRequest.from(routingContext.getBodyAsJson());

    findLoan(routingContext.getBodyAsJson(), openLoanFinder)
      .thenApply(openLoanResult -> HttpResult.combine(openLoanResult, requestResult,
        (loan, request) -> loan.checkIn(request.getCheckInDate(), request.getServicePointId())))
//      .thenComposeAsync(r -> r.after(loanCheckinService::checkin))
//      .thenComposeAsync(r -> r.after(updateItem::setLoansItemStatusAvaliable))
      .thenComposeAsync(result -> result.combineAfter(
        getRequestQueue(requestQueueRepository), this::addRequestQueue))
      .thenComposeAsync(result -> result.after(requestQueueUpdate::onCheckIn))
      .thenComposeAsync(result -> result.after(updateItem::onLoanUpdate))
      .thenComposeAsync(r -> r.after(loanRepository::updateLoan))
      .thenApply(r -> r.map(LoanAndRelatedRecords::getLoan))
      .thenApply(r -> r.map(loanRepresentation::extendedLoan))
      .thenApply(LoanResponse::from)
      .thenAccept(result -> result.writeTo(routingContext.response()));
  }

  private Function<Loan, CompletableFuture<HttpResult<RequestQueue>>> getRequestQueue(
    RequestQueueRepository requestQueueRepository) {

    return loan -> requestQueueRepository.get(loan.getItemId());
  }

  protected abstract CompletableFuture<HttpResult<Loan>> findLoan(
    JsonObject request,
    OpenLoanFinder loanFinder);

  private LoanAndRelatedRecords addRequestQueue(
    Loan loan,
    RequestQueue requestQueue) {

    return new LoanAndRelatedRecords(loan)
      .withRequestQueue(requestQueue);
  }
}
