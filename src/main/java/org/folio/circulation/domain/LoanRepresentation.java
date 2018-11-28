package org.folio.circulation.domain;

import org.folio.circulation.domain.representations.ItemSummaryRepresentation;

import io.vertx.core.json.JsonObject;

public class LoanRepresentation {
  public JsonObject extendedLoan(Loan loan) {
    if(loan == null) {
      return null;
    }

    return extendedLoan(loan.asJson(), loan.getItem());
  }

  private JsonObject extendedLoan(JsonObject loan, Item item) {
    //No need to pass on the itemStatus property, as only used to populate the history
    //and could be confused with aggregation of current status
    loan.remove("itemStatus");

    if(item != null && item.isFound()) {
      loan.put("item", new ItemSummaryRepresentation()
        .createItemSummary(item));
    }

    return loan;
  }
}
