package org.folio.circulation.domain.validation;

import static org.folio.circulation.support.HttpResult.of;

import java.util.function.Function;
import java.util.function.Supplier;

import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.MultipleRecords;
import org.folio.circulation.support.HttpFailure;
import org.folio.circulation.support.HttpResult;

public class MoreThanOneLoanValidator {
  private final Supplier<HttpFailure> failureSupplier;

  public MoreThanOneLoanValidator(Supplier<HttpFailure> failureSupplier) {
    this.failureSupplier = failureSupplier;
  }

  public HttpResult<MultipleRecords<Loan>> failWhenMoreThanOneLoan(
    HttpResult<MultipleRecords<Loan>> result) {

    return result.failWhen(moreThanOneLoan(),
      loans -> failureSupplier.get());
  }

  private static Function<MultipleRecords<Loan>, HttpResult<Boolean>> moreThanOneLoan() {
    return loans -> of(() -> loans.getTotalRecords() > 1);
  }
}
