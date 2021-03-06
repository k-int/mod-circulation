package org.folio.circulation.domain.validation;

import org.folio.circulation.domain.Request;
import org.folio.circulation.domain.RequestAndRelatedRecords;
import org.folio.circulation.domain.RequestRepository;
import org.folio.circulation.support.HttpResult;

import java.util.concurrent.CompletableFuture;

import static org.folio.circulation.support.HttpResult.succeeded;
import static org.folio.circulation.support.ValidationErrorFailure.failure;

public class ClosedRequestValidator {
  private final RequestRepository requestRepository;

  public ClosedRequestValidator(RequestRepository requestRepository) {
    this.requestRepository = requestRepository;
  }

  public CompletableFuture<HttpResult<RequestAndRelatedRecords>> refuseWhenAlreadyClosed(
    RequestAndRelatedRecords requestAndRelatedRecords) {

    return refuseWhenAlreadyClosed(requestAndRelatedRecords.getRequest())
      .thenApply(r -> r.map(v -> requestAndRelatedRecords));
  }

  private CompletableFuture<HttpResult<Request>> refuseWhenAlreadyClosed(Request request) {
    final String requestId = request.getId();

    return requestRepository.getById(requestId)
      .thenApply(r -> r.failWhen(existing -> succeeded(existing.isClosed()),
        v -> failure("Cannot edit a closed request", "id", requestId)));
  }

}
