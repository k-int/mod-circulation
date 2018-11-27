package org.folio.circulation.resources;

import java.util.concurrent.CompletableFuture;

import org.folio.circulation.domain.CheckInProcessRecords;
import org.folio.circulation.domain.Item;
import org.folio.circulation.domain.Loan;
import org.folio.circulation.storage.ItemByBarcodeInStorageFinder;
import org.folio.circulation.storage.SingleOpenLoanForItemInStorageFinder;
import org.folio.circulation.support.HttpResult;

class CheckInProcessAdapter {
  private final ItemByBarcodeInStorageFinder itemFinder;
  private final SingleOpenLoanForItemInStorageFinder singleOpenLoanFinder;

  CheckInProcessAdapter(
    ItemByBarcodeInStorageFinder itemFinder,
    SingleOpenLoanForItemInStorageFinder singleOpenLoanFinder) {

    this.itemFinder = itemFinder;
    this.singleOpenLoanFinder = singleOpenLoanFinder;
  }

  CompletableFuture<HttpResult<Item>> findItem(CheckInProcessRecords records) {
    return itemFinder.findItemByBarcode(records.getCheckInRequestBarcode());
  }

  CompletableFuture<HttpResult<Loan>> findSingleOpenLoan(
    CheckInProcessRecords records) {

    return singleOpenLoanFinder.findSingleOpenLoan(records.getItem());
  }
}
