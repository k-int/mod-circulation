package org.folio.circulation.domain;

import static org.folio.circulation.support.HttpResult.of;

import org.folio.circulation.support.HttpResult;

public class LoanCheckinService {
  public HttpResult<Loan> checkin(Loan loan) {
    return of(loan::checkin);
  }
}