package org.folio.circulation.resources;

import java.util.concurrent.CompletableFuture;

import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.OpenLoanFinder;
import org.folio.circulation.domain.representations.CheckInByBarcodeRequest;
import org.folio.circulation.support.HttpResult;

import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;

public class CheckInByBarcodeResource extends CheckInResource {
  public CheckInByBarcodeResource(HttpClient client) {
    super(client, "/circulation/check-in-by-barcode");
  }

  @Override
  protected CompletableFuture<HttpResult<Loan>> findLoan(
    JsonObject request,
    OpenLoanFinder loanFinder) {

    return CheckInByBarcodeRequest.from(request)
      .after(loanFinder::findByItemBarcode);
  }

}
