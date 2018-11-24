package org.folio.circulation.domain;

import static org.folio.circulation.support.HttpResult.of;

import java.util.concurrent.CompletableFuture;

import org.folio.circulation.domain.representations.CheckInByBarcodeRequest;
import org.folio.circulation.support.HttpResult;

public class LoanCheckInService {
  public CompletableFuture<HttpResult<Loan>> checkIn(
    HttpResult<CheckInByBarcodeRequest> checkInRequestResult,
    LoanRepository loanRepository) {

    return checkInRequestResult
      .after(loanRepository::findOpenLoanByBarcode)
      .thenApply(loanResult -> loanResult.combineToResult(checkInRequestResult,
        this::checkIn))
      .thenComposeAsync(result -> result.after(loanRepository::updateLoan));
  }

  private HttpResult<Loan> checkIn(
    Loan loan,
    CheckInByBarcodeRequest request) {

    return of(() -> loan.checkIn(
      request.getCheckInDate(),
      request.getServicePointId()));
  }
}
