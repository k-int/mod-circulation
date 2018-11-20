package org.folio.circulation.domain;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.circulation.support.HttpResult.failed;
import static org.folio.circulation.support.HttpResult.succeeded;
import static org.folio.circulation.support.ValidationErrorFailure.failedResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.folio.circulation.support.HttpResult;
import org.folio.circulation.support.ItemRepository;
import org.folio.circulation.support.ServerErrorFailure;

public class OpenLoanFinder {
  private final LoanRepository loanRepository;
  private final ItemRepository itemRepository;
  private final UserRepository userRepository;

  public OpenLoanFinder(
    LoanRepository loanRepository,
    ItemRepository itemRepository,
    UserRepository userRepository) {

    this.loanRepository = loanRepository;
    this.itemRepository = itemRepository;
    this.userRepository = userRepository;
  }

  //TODO: Consolidate with findOpenLoanByBarcode method in loan repository
  public CompletableFuture<HttpResult<Loan>> findByItemBarcode(FindByBarcodeQuery query) {
    return itemRepository.fetchByBarcode(query.getItemBarcode())
      .thenComposeAsync(itemResult -> itemResult.after(item -> {
        if(item.isNotFound()) {
          return completedFuture(failedResult(
            String.format("No item with barcode %s exists", query.getItemBarcode()),
            "itemBarcode", query.getItemBarcode()));
        }

        return loanRepository.findOpenLoans(item)
          .thenApply(checkSingleOpenLoan(item, query.getItemBarcode()));
      })).thenComposeAsync(this::fetchUser);
  }

  private Function<HttpResult<MultipleRecords<Loan>>, HttpResult<Loan>> checkSingleOpenLoan(
    Item item, String itemBarcode) {
    
    return loanResult -> loanResult.next(loans -> {
      final Optional<Loan> first = loans.getRecords().stream()
        .findFirst();

      if (loans.getTotalRecords() == 1 && first.isPresent()) {
        return succeeded(Loan.from(first.get().asJson(), item));
      } else {
        return failed(new ServerErrorFailure(
          String.format("More than one open loan for item %s", itemBarcode)));
      }
    });
  }

  private CompletableFuture<HttpResult<Loan>> fetchUser(HttpResult<Loan> result) {
    return result.combineAfter(userRepository::getUser, Loan::withUser);
  }
}
