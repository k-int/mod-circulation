package org.folio.circulation.api.support.fixtures;

import org.folio.circulation.api.support.builders.UserProxyBuilder;
import org.folio.circulation.api.support.http.ResourceClient;
import org.folio.circulation.support.http.client.IndividualResource;
import org.joda.time.*;

import java.net.MalformedURLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class UsersFixture {
  private final ResourceClient usersClient;
  private final ResourceClient userProxiesClient;

  public UsersFixture(
    ResourceClient usersClient,
    ResourceClient userProxiesClient) {

    this.usersClient = usersClient;
    this.userProxiesClient = userProxiesClient;
  }

  public void expiredProxyFor(
    IndividualResource sponsor,
    IndividualResource proxy)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    proxyFor(sponsor.getId(), proxy.getId(), DateTime.now().minusYears(1));
  }

  public void currentProxyFor(
    IndividualResource sponsor,
    IndividualResource proxy)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    proxyFor(sponsor.getId(), proxy.getId(), DateTime.now().plusYears(1));
  }

  public void proxyFor(
    UUID sponsorUserId,
    UUID proxyUserId,
    DateTime expirationDate)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    userProxiesClient.create(new UserProxyBuilder().
      withValidationFields(expirationDate.toString(), "Active",
        sponsorUserId.toString(), proxyUserId.toString()));
  }

  public void inactiveProxyFor(
    IndividualResource sponsor,
    IndividualResource proxy)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    userProxiesClient.create(new UserProxyBuilder().
      withValidationFields(DateTime.now().plusYears(1).toString(), "Inactive",
        sponsor.getId().toString(), proxy.getId().toString()));
  }

  public void nonExpiringProxyFor(
    IndividualResource sponsor,
    IndividualResource proxy)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {
      userProxiesClient.create(new UserProxyBuilder().
        withValidationFields(null, "Active",
          sponsor.getId().toString(), proxy.getId().toString()));
  }

  public IndividualResource jessica()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return usersClient.create(UserExamples.basedUponJessicaPontefract());
  }

  public IndividualResource james()
    throws
    InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return usersClient.create(UserExamples.basedUponJamesRodwell());
  }

  public IndividualResource rebecca()
    throws
    InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return usersClient.create(UserExamples.basedUponRebeccaStuart());
  }

  public IndividualResource steve()
    throws
    InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return usersClient.create(UserExamples.basedUponStevenJones());
  }

  public IndividualResource charlotte()
    throws
    InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return usersClient.create(UserExamples.basedUponCharlotteBroadwell());
  }
}
