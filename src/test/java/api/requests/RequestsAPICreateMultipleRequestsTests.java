package api.requests;

import api.support.APITests;
import api.support.builders.RequestBuilder;
import org.folio.circulation.support.http.client.IndividualResource;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class RequestsAPICreateMultipleRequestsTests extends APITests {

  @Test
  public void canCreateMultipleRequestsOfSameTypeForSameItem()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();

    loansFixture.checkOut(smallAngryPlanet, usersFixture.steve());

    final IndividualResource firstRequest = requestsClient.create(new RequestBuilder()
      .hold()
      .forItem(smallAngryPlanet)
      .by(usersFixture.jessica()));

    final IndividualResource secondRequest = requestsClient.create(new RequestBuilder()
      .hold()
      .forItem(smallAngryPlanet)
      .by(usersFixture.rebecca()));

    final IndividualResource thirdRequest = requestsClient.create(new RequestBuilder()
      .hold()
      .forItem(smallAngryPlanet)
      .by(usersFixture.charlotte()));

    assertThat(firstRequest.getJson().getInteger("position"), is(1));
    assertThat(secondRequest.getJson().getInteger("position"), is(2));
    assertThat(thirdRequest.getJson().getInteger("position"), is(3));
  }

  @Test
  public void canCreateMultipleRequestsOfDifferentTypeForSameItem()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();

    loansFixture.checkOut(smallAngryPlanet, usersFixture.rebecca());

    final IndividualResource firstRequest = requestsClient.create(new RequestBuilder()
      .hold()
      .forItem(smallAngryPlanet)
      .by(usersFixture.james()));

    final IndividualResource secondRequest = requestsClient.create(new RequestBuilder()
      .page()
      .forItem(smallAngryPlanet)
      .by(usersFixture.charlotte()));

    final IndividualResource thirdRequest = requestsClient.create(new RequestBuilder()
      .recall()
      .forItem(smallAngryPlanet)
      .by(usersFixture.steve()));

    assertThat(firstRequest.getJson().getInteger("position"), is(1));
    assertThat(secondRequest.getJson().getInteger("position"), is(2));
    assertThat(thirdRequest.getJson().getInteger("position"), is(3));
  }

  @Test
  public void canCreateMultipleRequestsAtSpecificLocation()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();

    loansFixture.checkOut(smallAngryPlanet, usersFixture.steve());

    final IndividualResource firstRequest = requestsClient.createAtSpecificLocation(
      new RequestBuilder()
        .open()
        .hold()
        .forItem(smallAngryPlanet)
        .by(usersFixture.jessica()));

    final IndividualResource secondRequest = requestsClient.createAtSpecificLocation(
      new RequestBuilder()
        .open()
        .hold()
        .forItem(smallAngryPlanet)
        .by(usersFixture.rebecca()));

    final IndividualResource thirdRequest = requestsClient.createAtSpecificLocation(
      new RequestBuilder()
        .open()
        .hold()
        .forItem(smallAngryPlanet)
        .by(usersFixture.charlotte()));

    assertThat("First request should have position",
      firstRequest.getJson().getInteger("position"), is(1));

    assertThat("Second request should have position",
      secondRequest.getJson().getInteger("position"), is(2));

    assertThat("Third request should have position",
      thirdRequest.getJson().getInteger("position"), is(3));
  }
}
