package api.loans;

import static api.support.matchers.UUIDMatcher.is;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import java.net.MalformedURLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.circulation.support.http.client.IndividualResource;
import org.folio.circulation.support.http.client.Response;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import api.support.APITests;
import io.vertx.core.json.JsonObject;

public class CheckInByBarcodeTests extends APITests {
  @Test
  public void canCloseAnOpenLoanByCheckingInTheItem()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    DateTime loanDate = new DateTime(2018, 3, 1, 13, 25, 46, DateTimeZone.UTC);

    final IndividualResource james = usersFixture.james();
    final IndividualResource nod = itemsFixture.basedUponNod();

    final UUID checkinServicePointId = UUID.randomUUID();

    final IndividualResource loan = loansFixture.checkOut(nod, james, loanDate);

    loansFixture.checkInLoan(loan.getId(),
      new DateTime(2018, 3, 5, 14, 23, 41, DateTimeZone.UTC),
      checkinServicePointId);

    Response updatedLoanResponse = loansClient.getById(loan.getId());

    JsonObject updatedLoan = updatedLoanResponse.getJson();

    assertThat(updatedLoan.getString("userId"), is(james.getId().toString()));

    assertThat("Should have return date",
      updatedLoan.getString("returnDate"), is("2018-03-05T14:23:41.000Z"));

    assertThat("status is not closed",
      updatedLoan.getJsonObject("status").getString("name"), is("Closed"));

    assertThat("action is not checkedin",
      updatedLoan.getString("action"), is("checkedin"));

    assertThat("title is taken from item",
      updatedLoan.getJsonObject("item").getString("title"),
      is("Nod"));

    assertThat("barcode is taken from item",
      updatedLoan.getJsonObject("item").getString("barcode"),
      is("565578437802"));

    assertThat("Should not have snapshot of item status, as current status is included",
      updatedLoan.containsKey("itemStatus"), is(false));

    JsonObject updatedNod = itemsClient.getById(nod.getId()).getJson();

    assertThat("item status is not available",
      updatedNod.getJsonObject("status").getString("name"), is("Available"));

    assertThat("item status snapshot in storage is not Available",
      loansStorageClient.getById(loan.getId()).getJson().getString("itemStatus"),
      is("Available"));

    assertThat("Checkin Service Point Id should be stored.",
      loansStorageClient.getById(loan.getId()).getJson().getString("checkinServicePointId"),
      is(checkinServicePointId));
  }
}
