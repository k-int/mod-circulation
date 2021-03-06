package api.loans;

import static api.APITestSuite.thirdFloorLocationId;
import static api.support.JsonCollectionAssistant.getRecordById;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.circulation.support.http.client.IndividualResource;
import org.folio.circulation.support.http.client.Response;
import org.junit.Ignore;
import org.junit.Test;

import api.support.APITests;
import api.support.builders.HoldingBuilder;
import api.support.builders.LoanBuilder;
import api.support.fixtures.InstanceExamples;
import api.support.fixtures.ItemExamples;
import io.vertx.core.json.JsonObject;

public class LoanAPIRelatedRecordsTests extends APITests {
  @Test
  public void holdingIdAndInstanceIdIncludedWhenHoldingAndInstanceAreAvailable()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    UUID instanceId = instancesClient.create(
      InstanceExamples.basedUponSmallAngryPlanet()).getId();

    UUID holdingId = holdingsClient.create(
      new HoldingBuilder()
        .forInstance(instanceId)
        .withPermanentLocation(thirdFloorLocationId())
        .create())
      .getId();

    UUID itemId = itemsClient.create(
      ItemExamples.basedUponSmallAngryPlanet()
        .forHolding(holdingId))
      .getId();

    UUID loanId = UUID.randomUUID();

    IndividualResource response = loansClient.create(new LoanBuilder()
      .withId(loanId)
      .withItemId(itemId));

    JsonObject createdLoan = response.getJson();

    assertThat("has holdings record ID",
      createdLoan.getJsonObject("item").containsKey("holdingsRecordId"), is(true));

    assertThat("has correct holdings record ID",
      createdLoan.getJsonObject("item").getString("holdingsRecordId"),
      is(holdingId.toString()));

    assertThat("has instance ID",
      createdLoan.getJsonObject("item").containsKey("instanceId"), is(true));

    assertThat("has correct instance ID",
      createdLoan.getJsonObject("item").getString("instanceId"),
      is(instanceId.toString()));

    Response fetchedLoanResponse = loansClient.getById(loanId);

    assertThat(fetchedLoanResponse.getStatusCode(), is(200));

    JsonObject fetchedLoan = fetchedLoanResponse.getJson();

    assertThat("has holdings record ID",
      fetchedLoan.getJsonObject("item").containsKey("holdingsRecordId"), is(true));

    assertThat("has correct holdings record ID",
      fetchedLoan.getJsonObject("item").getString("holdingsRecordId"),
      is(holdingId.toString()));

    assertThat("has instance ID",
      fetchedLoan.getJsonObject("item").containsKey("instanceId"), is(true));

    assertThat("has correct instance ID",
      fetchedLoan.getJsonObject("item").getString("instanceId"),
      is(instanceId.toString()));
  }

  @Ignore("mod-inventory-storage disallows this scenario, change to be isolated test")
  @Test
  public void holdingIdAndInstanceIdIncludedWhenInstanceNotAvailable()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    UUID instanceId = instancesClient.create(
      InstanceExamples.basedUponSmallAngryPlanet()).getId();

    UUID holdingId = holdingsClient.create(
      new HoldingBuilder()
        .forInstance(instanceId)
        .withPermanentLocation(thirdFloorLocationId())
        .create())
      .getId();

    UUID itemId = itemsClient.create(
      ItemExamples.basedUponSmallAngryPlanet()
        .forHolding(holdingId))
      .getId();

    UUID loanId = UUID.randomUUID();

    loansClient.create(new LoanBuilder()
      .withId(loanId)
      .withItemId(itemId));

    instancesClient.delete(instanceId);

    Response fetchedLoanResponse = loansClient.getById(loanId);

    assertThat(fetchedLoanResponse.getStatusCode(), is(200));

    JsonObject fetchedLoan = fetchedLoanResponse.getJson();

    assertThat("has holdings record ID",
      fetchedLoan.getJsonObject("item").containsKey("holdingsRecordId"), is(true));

    assertThat("has correct holdings record ID",
      fetchedLoan.getJsonObject("item").getString("holdingsRecordId"),
      is(holdingId.toString()));

    assertThat("has instance ID",
      fetchedLoan.getJsonObject("item").containsKey("instanceId"), is(true));

    assertThat("has correct instance ID",
      fetchedLoan.getJsonObject("item").getString("instanceId"),
      is(instanceId.toString()));
  }

  @Ignore("mod-inventory-storage disallows this scenario, change to be isolated test")
  @Test
  public void noInstanceIdIncludedWhenHoldingNotAvailable()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    UUID instanceId = instancesClient.create(
      InstanceExamples.basedUponSmallAngryPlanet()).getId();

    UUID holdingId = holdingsClient.create(
      new HoldingBuilder()
        .forInstance(instanceId)
        .withPermanentLocation(thirdFloorLocationId())
        .create())
      .getId();

    UUID itemId = itemsClient.create(
      ItemExamples.basedUponSmallAngryPlanet()
        .forHolding(holdingId))
      .getId();

    UUID loanId = UUID.randomUUID();

    loansClient.create(new LoanBuilder()
      .withId(loanId)
      .withItemId(itemId));

    holdingsClient.delete(holdingId);

    Response fetchedLoanResponse = loansClient.getById(loanId);

    assertThat(fetchedLoanResponse.getStatusCode(), is(200));

    JsonObject fetchedLoan = fetchedLoanResponse.getJson();

    assertThat("has holdings record ID",
      fetchedLoan.getJsonObject("item").containsKey("holdingsRecordId"), is(true));

    assertThat("has correct holdings record ID",
      fetchedLoan.getJsonObject("item").getString("holdingsRecordId"),
      is(holdingId.toString()));

    assertThat("has no instance ID",
      fetchedLoan.getJsonObject("item").containsKey("instanceId"), is(false));
  }

  @Test
  public void holdingAndInstanceIdComesFromMultipleRecordsForMultipleLoans()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    UUID firstInstanceId = instancesClient.create(
      InstanceExamples.basedUponSmallAngryPlanet()).getId();

    UUID firstHoldingId = holdingsClient.create(
      new HoldingBuilder()
        .forInstance(firstInstanceId)
        .withPermanentLocation(thirdFloorLocationId())
        .create())
      .getId();

    UUID firstItemId = itemsClient.create(
      ItemExamples.basedUponSmallAngryPlanet()
        .forHolding(firstHoldingId))
      .getId();

    UUID secondInstanceId = instancesClient.create(
      InstanceExamples.basedUponTemeraire()).getId();

    UUID secondHoldingId = holdingsClient.create(
      new HoldingBuilder()
        .forInstance(secondInstanceId)
        .withPermanentLocation(thirdFloorLocationId())
        .create())
      .getId();

    UUID secondItemId = itemsClient.create(
      ItemExamples.basedUponTemeraire()
        .forHolding(secondHoldingId))
      .getId();

    UUID firstLoanId = loansClient.create(new LoanBuilder()
      .withItemId(firstItemId)).getId();

    UUID secondLoanId = loansClient.create(new LoanBuilder()
      .withItemId(secondItemId)).getId();

    List<JsonObject> fetchedLoansResponse = loansClient.getAll();

    JsonObject firstFetchedLoan = getRecordById(
      fetchedLoansResponse, firstLoanId).get();

    JsonObject secondFetchedLoan = getRecordById(
      fetchedLoansResponse, secondLoanId).get();

    assertThat("has holdings record ID",
      firstFetchedLoan.getJsonObject("item").containsKey("holdingsRecordId"), is(true));

    assertThat("has correct holdings record ID",
      firstFetchedLoan.getJsonObject("item").getString("holdingsRecordId"),
      is(firstHoldingId.toString()));

    assertThat("has instance ID",
      firstFetchedLoan.getJsonObject("item").containsKey("instanceId"), is(true));

    assertThat("has correct instance ID",
      firstFetchedLoan.getJsonObject("item").getString("instanceId"),
      is(firstInstanceId.toString()));

    assertThat("has holdings record ID",
      secondFetchedLoan.getJsonObject("item").containsKey("holdingsRecordId"), is(true));

    assertThat("has correct holdings record ID",
      secondFetchedLoan.getJsonObject("item").getString("holdingsRecordId"),
      is(secondHoldingId.toString()));

    assertThat("has instance ID",
      secondFetchedLoan.getJsonObject("item").containsKey("instanceId"), is(true));

    assertThat("has correct instance ID",
      secondFetchedLoan.getJsonObject("item").getString("instanceId"),
      is(secondInstanceId.toString()));
  }

  @Ignore("mod-inventory-storage disallows this scenario, change to be isolated test")
  @Test
  public void noInstanceIdForMultipleLoansWhenHoldingNotFound()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    UUID firstInstanceId = instancesClient.create(
      InstanceExamples.basedUponSmallAngryPlanet()).getId();

    UUID firstHoldingId = holdingsClient.create(
      new HoldingBuilder()
        .forInstance(firstInstanceId)
        .withPermanentLocation(thirdFloorLocationId())
        .create())
      .getId();

    UUID firstItemId = itemsClient.create(
      ItemExamples.basedUponSmallAngryPlanet()
        .forHolding(firstHoldingId))
      .getId();

    UUID secondInstanceId = instancesClient.create(
      InstanceExamples.basedUponTemeraire()).getId();

    UUID secondHoldingId = holdingsClient.create(
      new HoldingBuilder()
        .forInstance(secondInstanceId)
        .withPermanentLocation(thirdFloorLocationId())
        .create())
      .getId();

    UUID secondItemId = itemsClient.create(
      ItemExamples.basedUponTemeraire()
        .forHolding(secondHoldingId))
      .getId();

    UUID firstLoanId = loansClient.create(new LoanBuilder()
      .withItemId(firstItemId)).getId();

    UUID secondLoanId = loansClient.create(new LoanBuilder()
      .withItemId(secondItemId)).getId();

    //Delete a holding
    holdingsClient.delete(secondHoldingId);

    List<JsonObject> fetchedLoansResponse = loansClient.getAll();

    JsonObject firstFetchedLoan = getRecordById(
      fetchedLoansResponse, firstLoanId).get();

    JsonObject secondFetchedLoan = getRecordById(
      fetchedLoansResponse, secondLoanId).get();

    assertThat("first loan has instance ID",
      firstFetchedLoan.getJsonObject("item").containsKey("instanceId"), is(true));

    assertThat("second loan does not have instance ID",
      secondFetchedLoan.getJsonObject("item").containsKey("instanceId"), is(false));
  }

  @Ignore("mod-inventory-storage disallows this scenario, change to be isolated test")
  @Test
  public void instanceIdAndHoldingIdForMultipleLoansWhenInstanceNotFound()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    UUID firstInstanceId = instancesClient.create(
      InstanceExamples.basedUponSmallAngryPlanet()).getId();

    UUID firstHoldingId = holdingsClient.create(
      new HoldingBuilder()
        .forInstance(firstInstanceId)
        .withPermanentLocation(thirdFloorLocationId())
        .create())
      .getId();

    UUID firstItemId = itemsClient.create(
      ItemExamples.basedUponSmallAngryPlanet()
        .forHolding(firstHoldingId))
      .getId();

    UUID secondInstanceId = instancesClient.create(
      InstanceExamples.basedUponTemeraire()).getId();

    UUID secondHoldingId = holdingsClient.create(
      new HoldingBuilder()
        .forInstance(secondInstanceId)
        .withPermanentLocation(thirdFloorLocationId())
        .create())
      .getId();

    UUID secondItemId = itemsClient.create(
      ItemExamples.basedUponTemeraire()
        .forHolding(secondHoldingId))
      .getId();

    UUID firstLoanId = loansClient.create(new LoanBuilder()
      .withItemId(firstItemId)).getId();

    UUID secondLoanId = loansClient.create(new LoanBuilder()
      .withItemId(secondItemId)).getId();

    instancesClient.delete(firstInstanceId);
    instancesClient.delete(secondInstanceId);

    List<JsonObject> fetchedLoansResponse = loansClient.getAll();

    JsonObject firstFetchedLoan = getRecordById(
      fetchedLoansResponse, firstLoanId).get();

    JsonObject secondFetchedLoan = getRecordById(
      fetchedLoansResponse, secondLoanId).get();

    assertThat("first loan has instance ID",
      firstFetchedLoan.getJsonObject("item").containsKey("instanceId"), is(true));

    assertThat("second loan has instance ID",
      secondFetchedLoan.getJsonObject("item").containsKey("instanceId"), is(true));
  }
}
