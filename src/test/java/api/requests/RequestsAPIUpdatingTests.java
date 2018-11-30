package api.requests;

import static api.APITestSuite.workAddressTypeId;
import static api.support.matchers.ResponseStatusCodeMatcher.hasStatus;
import static api.support.matchers.TextDateTimeMatcher.isEquivalentTo;
import static api.support.matchers.ValidationErrorMatchers.hasErrorWith;
import static api.support.matchers.ValidationErrorMatchers.hasMessage;
import static api.support.matchers.ValidationErrorMatchers.hasParameter;
import static org.folio.HttpStatus.HTTP_VALIDATION_ERROR;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.folio.circulation.support.http.client.IndividualResource;
import org.folio.circulation.support.http.client.Response;
import org.folio.circulation.support.http.client.ResponseHandler;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;

import api.support.APITests;
import api.support.builders.Address;
import api.support.builders.ItemBuilder;
import api.support.builders.RequestBuilder;
import api.support.builders.UserBuilder;
import api.support.http.InterfaceUrls;
import io.vertx.core.json.JsonObject;

public class RequestsAPIUpdatingTests extends APITests {
  @Test
  public void canReplaceAnExistingRequest()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    UUID id = UUID.randomUUID();

    UUID itemId = itemsFixture.basedUponTemeraire(
      itemRequestBuilder -> itemRequestBuilder.withBarcode("07295629642"))
      .getId();

    loansFixture.checkOutItem(itemId);

    UUID originalRequesterId = usersClient.create(new UserBuilder()
      .withName("Norton", "Jessica")
      .withBarcode("764523186496"))
      .getId();

    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);

    final IndividualResource exampleServicePoint = servicePointsFixture.cd1();

    //TODO: Should include pickup service point
    IndividualResource createdRequest = requestsClient.create(
      new RequestBuilder()
      .recall()
      .withId(id)
      .withRequestDate(requestDate)
      .withItemId(itemId)
      .withRequesterId(originalRequesterId)
      .fulfilToHoldShelf()
      .withPickupServicePointId(exampleServicePoint.getId())
      .withRequestExpiration(new LocalDate(2017, 7, 30))
      .withHoldShelfExpiration(new LocalDate(2017, 8, 31)));

    UUID updatedRequester = usersClient.create(new UserBuilder()
      .withName("Campbell", "Fiona")
      .withBarcode("679231693475"))
      .getId();

    JsonObject updatedRequest = requestsClient.getById(createdRequest.getId())
      .getJson();

    updatedRequest
      .put("requestType", "Hold")
      .put("requesterId", updatedRequester.toString());

    CompletableFuture<Response> putCompleted = new CompletableFuture<>();

    client.put(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      updatedRequest, ResponseHandler.any(putCompleted));

    Response putResponse = putCompleted.get(5, TimeUnit.SECONDS);

    assertThat(putResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    CompletableFuture<Response> getCompleted = new CompletableFuture<>();

    client.get(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      ResponseHandler.any(getCompleted));

    Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

    assertThat(String.format("Failed to get request: %s", getResponse.getBody()),
      getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject representation = getResponse.getJson();

    assertThat(representation.getString("id"), is(id.toString()));
    assertThat(representation.getString("requestType"), is("Hold"));
    assertThat(representation.getString("requestDate"), isEquivalentTo(requestDate));
    assertThat(representation.getString("itemId"), is(itemId.toString()));
    assertThat(representation.getString("requesterId"), is(updatedRequester.toString()));
    assertThat(representation.getString("fulfilmentPreference"), is("Hold Shelf"));
    assertThat(representation.getString("requestExpirationDate"), is("2017-07-30"));
    assertThat(representation.getString("holdShelfExpirationDate"), is("2017-08-31"));

    assertThat("has information taken from item",
      representation.containsKey("item"), is(true));

    assertThat("title is taken from item",
      representation.getJsonObject("item").getString("title"),
      is("Temeraire"));

    assertThat("barcode is taken from item",
      representation.getJsonObject("item").getString("barcode"),
      is("07295629642"));

    assertThat("has information taken from requesting user",
      representation.containsKey("requester"), is(true));

    assertThat("last name is taken from requesting user",
      representation.getJsonObject("requester").getString("lastName"),
      is("Campbell"));

    assertThat("first name is taken from requesting user",
      representation.getJsonObject("requester").getString("firstName"),
      is("Fiona"));

    assertThat("middle name is not taken from requesting user",
      representation.getJsonObject("requester").containsKey("middleName"),
      is(false));

    assertThat("barcode is taken from requesting user",
      representation.getJsonObject("requester").getString("barcode"),
      is("679231693475"));
  }
  
  @Test
  public void canReplaceAnExistingRequestWithDeliveryAddress() 
      throws InterruptedException,
      MalformedURLException,
      TimeoutException,
      ExecutionException {
    
    UUID id = UUID.randomUUID();

    UUID itemId = itemsFixture.basedUponTemeraire(
      itemRequestBuilder -> itemRequestBuilder.withBarcode("07295629642"))
      .getId();

    loansFixture.checkOutItem(itemId);

    UUID requesterId = usersClient.create(new UserBuilder()
      .withName("Campbell", "Fiona")
      .withBarcode("679231693475")
      .withAddress(
        new Address(workAddressTypeId(),
          "Fake first address line",
          "Fake second address line",
          "Fake city",
          "Fake region",
          "Fake postal code",
          "Fake country code")))
      .getId();

    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);

    final IndividualResource exampleServicePoint = servicePointsFixture.cd1();

    IndividualResource createdRequest = requestsClient.create(
      new RequestBuilder()
      .recall()
      .withId(id)
      .withRequestDate(requestDate)
      .withItemId(itemId)
      .withRequesterId(requesterId)
      .withPickupServicePointId(exampleServicePoint.getId())
      .withRequestExpiration(new LocalDate(2017, 7, 30))
      .deliverToAddress(workAddressTypeId()));

    JsonObject updatedRequest = requestsClient.getById(createdRequest.getId())
      .getJson();

    updatedRequest
      .put("requestType", "Hold");

    CompletableFuture<Response> putCompleted = new CompletableFuture<>();

    client.put(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      updatedRequest, ResponseHandler.any(putCompleted));

    Response putResponse = putCompleted.get(5, TimeUnit.SECONDS);

    assertThat(putResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    CompletableFuture<Response> getCompleted = new CompletableFuture<>();

    client.get(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      ResponseHandler.any(getCompleted));

    Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

    assertThat(String.format("Failed to get request: %s", getResponse.getBody()),
      getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject representation = getResponse.getJson();

    assertThat(representation.getString("id"), is(id.toString()));
    assertThat(representation.getString("requestType"), is("Hold"));
    assertThat(representation.getString("requestDate"), isEquivalentTo(requestDate));
    assertThat(representation.getString("itemId"), is(itemId.toString()));
    assertThat(representation.getString("fulfilmentPreference"), is("Delivery"));
    assertThat(representation.getString("requestExpirationDate"), is("2017-07-30"));

    assertThat("has information taken from item",
      representation.containsKey("item"), is(true));

    assertThat("title is taken from item",
      representation.getJsonObject("item").getString("title"),
      is("Temeraire"));

    assertThat("barcode is taken from item",
      representation.getJsonObject("item").getString("barcode"),
      is("07295629642"));

    assertThat("has information taken from requesting user",
      representation.containsKey("requester"), is(true));

    assertThat("last name is taken from requesting user",
      representation.getJsonObject("requester").getString("lastName"),
      is("Campbell"));

    assertThat("first name is taken from requesting user",
      representation.getJsonObject("requester").getString("firstName"),
      is("Fiona"));

    assertThat("middle name is not taken from requesting user",
      representation.getJsonObject("requester").containsKey("middleName"),
      is(false));

    assertThat("barcode is taken from requesting user",
      representation.getJsonObject("requester").getString("barcode"),
      is("679231693475"));

    assertThat("Request should have a delivery address",
      representation.containsKey("deliveryAddress"), is(true));

    final JsonObject deliveryAddress = representation.getJsonObject("deliveryAddress");

    assertThat(deliveryAddress.getString("addressTypeId"), is(workAddressTypeId().toString()));
    assertThat(deliveryAddress.getString("addressLine1"), is("Fake first address line"));
    assertThat(deliveryAddress.getString("addressLine2"), is("Fake second address line"));
    assertThat(deliveryAddress.getString("city"), is("Fake city"));
    assertThat(deliveryAddress.getString("region"), is("Fake region"));
    assertThat(deliveryAddress.getString("postalCode"), is("Fake postal code"));
    assertThat(deliveryAddress.getString("countryId"), is("Fake country code"));
  }

  @Test
  public void replacingAnExistingRequestRemovesItemInformationWhenItemDoesNotExist()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    UUID id = UUID.randomUUID();

    UUID itemId = itemsFixture.basedUponNod().getId();

    loansFixture.checkOutItem(itemId);

    UUID requesterId = usersClient.create(new UserBuilder()).getId();

    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);

    IndividualResource createdRequest = requestsClient.create(
      new RequestBuilder()
        .recall()
        .withId(id)
        .withRequestDate(requestDate)
        .withItemId(itemId)
        .withRequesterId(requesterId)
        .fulfilToHoldShelf()
        .withRequestExpiration(new LocalDate(2017, 7, 30))
        .withHoldShelfExpiration(new LocalDate(2017, 8, 31)));

    JsonObject updatedRequest = requestsClient.getById(createdRequest.getId())
      .getJson();

    itemsClient.delete(itemId);

    updatedRequest
      .put("requestType", "Hold");

    CompletableFuture<Response> putCompleted = new CompletableFuture<>();

    client.put(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      updatedRequest, ResponseHandler.any(putCompleted));

    Response putResponse = putCompleted.get(5, TimeUnit.SECONDS);

    assertThat(String.format("Should replace request: %s", putResponse.getBody()),
      putResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    CompletableFuture<Response> getCompleted = new CompletableFuture<>();

    client.get(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      ResponseHandler.any(getCompleted));

    Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

    assertThat(String.format("Failed to get request: %s", getResponse.getBody()),
      getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject representation = getResponse.getJson();

    assertThat(representation.getString("itemId"), is(itemId.toString()));

    assertThat("has no item information when item no longer exists",
      representation.containsKey("item"), is(false));
  }

  @Test
  public void replacingAnExistingRequestRemovesRequesterInformationWhenUserDoesNotExist()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    UUID id = UUID.randomUUID();

    UUID itemId = itemsFixture.basedUponNod().getId();

    loansFixture.checkOutItem(itemId);

    UUID requester = usersClient.create(new UserBuilder()
      .withName("Norton", "Jessica")
      .withBarcode("679231693475"))
      .getId();

    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);

    IndividualResource createdRequest = requestsClient.create(
      new RequestBuilder()
        .recall()
        .withId(id)
        .withRequestDate(requestDate)
        .withItemId(itemId)
        .withRequesterId(requester)
        .fulfilToHoldShelf()
        .withRequestExpiration(new LocalDate(2017, 7, 30))
        .withHoldShelfExpiration(new LocalDate(2017, 8, 31)));

    usersClient.delete(requester);

    JsonObject updatedRequest = requestsClient.getById(createdRequest.getId())
      .getJson();

    updatedRequest
      .put("requestType", "Hold");

    CompletableFuture<Response> putCompleted = new CompletableFuture<>();

    client.put(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      updatedRequest, ResponseHandler.any(putCompleted));

    Response putResponse = putCompleted.get(5, TimeUnit.SECONDS);

    assertThat(putResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    CompletableFuture<Response> getCompleted = new CompletableFuture<>();

    client.get(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      ResponseHandler.any(getCompleted));

    Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

    assertThat(String.format("Failed to get request: %s", getResponse.getBody()),
      getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject representation = getResponse.getJson();

    assertThat(representation.getString("requesterId"), is(requester.toString()));

    assertThat("has no requesting user information taken when user no longer exists",
      representation.containsKey("requester"), is(false));
  }

  @Test
  public void replacingAnExistingRequestRemovesRequesterBarcodeWhenNonePresent()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    UUID id = UUID.randomUUID();

    UUID itemId = itemsFixture.basedUponNod().getId();

    loansFixture.checkOutItem(itemId);

    UUID originalRequesterId = usersClient.create(new UserBuilder()
      .withName("Norton", "Jessica")
      .withBarcode("764523186496"))
      .getId();

    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);

    IndividualResource createdRequest = requestsClient.create(
      new RequestBuilder()
        .recall()
        .withId(id)
        .withRequestDate(requestDate)
        .withItemId(itemId)
        .withRequesterId(originalRequesterId)
        .fulfilToHoldShelf()
        .withRequestExpiration(new LocalDate(2017, 7, 30))
        .withHoldShelfExpiration(new LocalDate(2017, 8, 31)));

    UUID updatedRequester = usersClient.create(new UserBuilder()
      .withName("Campbell", "Fiona")
      .withNoBarcode())
      .getId();

    JsonObject updatedRequest = requestsClient.getById(createdRequest.getId())
      .getJson();

    updatedRequest
      .put("requestType", "Hold")
      .put("requesterId", updatedRequester.toString());

    CompletableFuture<Response> putCompleted = new CompletableFuture<>();

    client.put(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      updatedRequest, ResponseHandler.any(putCompleted));

    Response putResponse = putCompleted.get(5, TimeUnit.SECONDS);

    assertThat(putResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    CompletableFuture<Response> getCompleted = new CompletableFuture<>();

    client.get(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      ResponseHandler.any(getCompleted));

    Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

    assertThat(String.format("Failed to get request: %s", getResponse.getBody()),
      getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject representation = getResponse.getJson();

    assertThat(representation.getString("requesterId"), is(updatedRequester.toString()));

    assertThat("has information taken from requesting user",
      representation.containsKey("requester"), is(true));

    assertThat("last name is taken from requesting user",
      representation.getJsonObject("requester").getString("lastName"),
      is("Campbell"));

    assertThat("first name is taken from requesting user",
      representation.getJsonObject("requester").getString("firstName"),
      is("Fiona"));

    assertThat("barcode is not present when requesting user does not have one",
      representation.getJsonObject("requester").containsKey("barcode"),
      is(false));
  }

  @Test
  public void replacingAnExistingRequestIncludesRequesterMiddleNameWhenPresent()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    UUID id = UUID.randomUUID();

    UUID itemId = itemsFixture.basedUponTemeraire(
      itemRequestBuilder -> itemRequestBuilder.withBarcode("07295629642"))
      .getId();

    loansFixture.checkOutItem(itemId);

    UUID originalRequesterId = usersClient.create(new UserBuilder()
      .withName("Norton", "Jessica")
      .withBarcode("764523186496"))
      .getId();

    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);

    IndividualResource createdRequest = requestsClient.create(
      new RequestBuilder()
      .recall()
      .withId(id)
      .withRequestDate(requestDate)
      .withItemId(itemId)
      .withRequesterId(originalRequesterId)
      .fulfilToHoldShelf()
      .withRequestExpiration(new LocalDate(2017, 7, 30))
      .withHoldShelfExpiration(new LocalDate(2017, 8, 31)));

    UUID updatedRequester = usersClient.create(new UserBuilder()
      .withName("Campbell", "Fiona", "Stella")
      .withBarcode("679231693475")).getId();

    JsonObject updatedRequest = requestsClient.getById(createdRequest.getId())
      .getJson();

    updatedRequest
      .put("requestType", "Hold")
      .put("requesterId", updatedRequester.toString());

    CompletableFuture<Response> putCompleted = new CompletableFuture<>();

    client.put(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      updatedRequest, ResponseHandler.any(putCompleted));

    Response putResponse = putCompleted.get(5, TimeUnit.SECONDS);

    assertThat(putResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    CompletableFuture<Response> getCompleted = new CompletableFuture<>();

    client.get(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      ResponseHandler.any(getCompleted));

    Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

    assertThat(String.format("Failed to get request: %s", getResponse.getBody()),
      getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject representation = getResponse.getJson();

    assertThat(representation.getString("id"), is(id.toString()));
    assertThat(representation.getString("requestType"), is("Hold"));
    assertThat(representation.getString("requestDate"), isEquivalentTo(requestDate));
    assertThat(representation.getString("itemId"), is(itemId.toString()));
    assertThat(representation.getString("requesterId"), is(updatedRequester.toString()));
    assertThat(representation.getString("fulfilmentPreference"), is("Hold Shelf"));
    assertThat(representation.getString("requestExpirationDate"), is("2017-07-30"));
    assertThat(representation.getString("holdShelfExpirationDate"), is("2017-08-31"));

    assertThat("has information taken from item",
      representation.containsKey("item"), is(true));

    assertThat("title is taken from item",
      representation.getJsonObject("item").getString("title"),
      is("Temeraire"));

    assertThat("barcode is taken from item",
      representation.getJsonObject("item").getString("barcode"),
      is("07295629642"));

    assertThat("has information taken from requesting user",
      representation.containsKey("requester"), is(true));

    assertThat("last name is taken from requesting user",
      representation.getJsonObject("requester").getString("lastName"),
      is("Campbell"));

    assertThat("first name is taken from requesting user",
      representation.getJsonObject("requester").getString("firstName"),
      is("Fiona"));

    assertThat("middle name is taken from requesting user",
      representation.getJsonObject("requester").getString("middleName"),
      is("Stella"));

    assertThat("barcode is taken from requesting user",
      representation.getJsonObject("requester").getString("barcode"),
      is("679231693475"));
  }

  @Test
  public void replacingAnExistingRequestRemovesItemBarcodeWhenNonePresent()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    UUID id = UUID.randomUUID();

    UUID originalItemId = itemsFixture.basedUponTemeraire().getId();

    loansFixture.checkOutItem(originalItemId);

    UUID requesterId = usersClient.create(new UserBuilder()).getId();

    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);

    IndividualResource createdRequest = requestsClient.create(
      new RequestBuilder()
      .recall()
      .withId(id)
      .withRequestDate(requestDate)
      .withItemId(originalItemId)
      .withRequesterId(requesterId)
      .fulfilToHoldShelf()
      .withRequestExpiration(new LocalDate(2017, 7, 30))
      .withHoldShelfExpiration(new LocalDate(2017, 8, 31)));

    UUID updatedItemId = itemsFixture.basedUponSmallAngryPlanet(ItemBuilder::withNoBarcode)
      .getId();

    JsonObject updatedRequest = requestsClient.getById(createdRequest.getId())
      .getJson();

    updatedRequest
      .put("itemId", updatedItemId.toString());

    CompletableFuture<Response> putCompleted = new CompletableFuture<>();

    client.put(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      updatedRequest, ResponseHandler.any(putCompleted));

    Response putResponse = putCompleted.get(5, TimeUnit.SECONDS);

    assertThat(putResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    CompletableFuture<Response> getCompleted = new CompletableFuture<>();

    client.get(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      ResponseHandler.any(getCompleted));

    Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

    assertThat(String.format("Failed to get request: %s", getResponse.getBody()),
      getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject representation = getResponse.getJson();

    assertThat(representation.getString("itemId"), is(updatedItemId.toString()));

    assertThat("has information taken from item",
      representation.containsKey("item"), is(true));

    assertThat("title is taken from item",
      representation.getJsonObject("item").getString("title"),
      is("The Long Way to a Small, Angry Planet"));

    assertThat("barcode is not taken from item",
      representation.getJsonObject("item").containsKey("barcode"),
      is(false));
  }
  
  @Test
  public void cannotReplaceAnExistingRequestWithInvalidPickupLocation()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    UUID id = UUID.randomUUID();

    UUID itemId = itemsFixture.basedUponTemeraire(
      itemRequestBuilder -> itemRequestBuilder.withBarcode("07295629642"))
      .getId();

    loansFixture.checkOutItem(itemId);

    UUID requesterId = usersFixture.jessica().getId();

    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);

    final IndividualResource exampleServicePoint = servicePointsFixture.cd1();

    IndividualResource createdRequest = requestsClient.create(
      new RequestBuilder()
      .recall()
      .withId(id)
      .withRequestDate(requestDate)
      .withItemId(itemId)
      .withRequesterId(requesterId)
      .fulfilToHoldShelf()
      .withPickupServicePointId(exampleServicePoint.getId())
      .withRequestExpiration(new LocalDate(2017, 7, 30))
      .withHoldShelfExpiration(new LocalDate(2017, 8, 31)));

    JsonObject updatedRequest = requestsClient.getById(createdRequest.getId())
      .getJson();

     UUID badServicePointId = servicePointsFixture.cd3().getId();

    updatedRequest
      .put("pickupServicePointId", badServicePointId.toString());

    CompletableFuture<Response> putCompleted = new CompletableFuture<>();

    client.put(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      updatedRequest, ResponseHandler.any(putCompleted));

    Response putResponse = putCompleted.get(5, TimeUnit.SECONDS);

    assertThat(putResponse, hasStatus(HTTP_VALIDATION_ERROR));

   assertThat(putResponse.getJson(), hasErrorWith(allOf(
     hasMessage("Service point is not a pickup location"),
     hasParameter("pickupServicePointId", badServicePointId.toString()))));
  }

  @Test
  public void cannotReplaceAnExistingRequestWithUnknownPickupLocation()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    UUID id = UUID.randomUUID();

    UUID itemId = itemsFixture.basedUponTemeraire(
      itemRequestBuilder -> itemRequestBuilder.withBarcode("07295629642"))
      .getId();

    loansFixture.checkOutItem(itemId);

    UUID requesterId = usersFixture.jessica().getId();

    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);

    final IndividualResource exampleServicePoint = servicePointsFixture.cd1();

    IndividualResource createdRequest = requestsClient.create(
      new RequestBuilder()
        .recall()
        .withId(id)
        .withRequestDate(requestDate)
        .withItemId(itemId)
        .withRequesterId(requesterId)
        .fulfilToHoldShelf()
        .withPickupServicePointId(exampleServicePoint.getId())
        .withRequestExpiration(new LocalDate(2017, 7, 30))
        .withHoldShelfExpiration(new LocalDate(2017, 8, 31)));

    JsonObject updatedRequest = requestsClient.getById(createdRequest.getId())
      .getJson();

    UUID badServicePointId = UUID.randomUUID();

    updatedRequest
      .put("pickupServicePointId", badServicePointId.toString());

    CompletableFuture<Response> putCompleted = new CompletableFuture<>();

    client.put(InterfaceUrls.requestsUrl(String.format("/%s", id)),
      updatedRequest, ResponseHandler.any(putCompleted));

    Response putResponse = putCompleted.get(5, TimeUnit.SECONDS);

    assertThat(putResponse, hasStatus(HTTP_VALIDATION_ERROR));

    assertThat(putResponse.getJson(), hasErrorWith(allOf(
      hasMessage("Pickup service point does not exist"),
      hasParameter("pickupServicePointId", badServicePointId.toString()))));
  }
}
