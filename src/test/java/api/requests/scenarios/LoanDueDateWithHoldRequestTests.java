package api.requests.scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.MalformedURLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.circulation.domain.policy.Period;
import org.folio.circulation.support.ClockManager;
import org.folio.circulation.support.http.client.IndividualResource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import api.support.APITests;
import api.support.builders.LoanPolicyBuilder;
import io.vertx.core.json.JsonObject;

public class LoanDueDateWithHoldRequestTests extends APITests {
  private static Clock clock;

  @BeforeClass
  public static void setUpBeforeClass() {
    clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
  }

  @Before
  public void setUp() {
    // reset the clock before each test (just in case)
    ClockManager.getClockManager().setClock(clock);
  }

  @Test
  public void dueDateBasedUponAlternativePeriodWhenItemIsOnHold()
    throws
    MalformedURLException,
    InterruptedException,
    TimeoutException,
    ExecutionException {

    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();
    final IndividualResource charlotte = usersFixture.charlotte();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
      .withName("Alternative period when hold requested")
      .rolling(Period.weeks(3))
      .unlimitedRenewals()
      .renewFromSystemDate()
      .withAlternateCheckOutPeriodForItemsOnHold(Period.weeks(2));

    final IndividualResource loanPolicy = loanPoliciesFixture.create(canCirculateRollingPolicy);

    useLoanPolicyAsFallback(loanPolicy.getId(),
      requestPoliciesFixture.allowAllRequestPolicy().getId(),
      noticePoliciesFixture.activeNotice().getId());

    loansFixture.checkOutByBarcode(smallAngryPlanet, charlotte,
      DateTime.now(DateTimeZone.UTC));

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, steve,
      DateTime.now(DateTimeZone.UTC), requestServicePoint.getId(), "Hold");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
      DateTime.now(DateTimeZone.UTC), requestServicePoint.getId(), "Hold");

    loansFixture.checkInByBarcode(smallAngryPlanet);

    final IndividualResource loan = loansFixture.checkOutByBarcode(
      smallAngryPlanet, steve, DateTime.now(DateTimeZone.UTC));

    final JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    final String expectedDueDate = ClockManager.getClockManager()
      .getDateTime().plusWeeks(2).toString(ISODateTimeFormat.dateTime());

    assertThat("due date is not the recall due date (2 months)",
      storedLoan.getString("dueDate"), is(expectedDueDate));
  }
}
