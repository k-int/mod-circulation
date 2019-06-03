package api.loans;

import static api.support.matchers.PatronNoticeMatcher.hasEmailNoticeProperties;
import static api.support.matchers.ScheduledNoticeMatchers.hasScheduledLoanNotice;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.folio.circulation.domain.policy.Period;
import org.folio.circulation.support.http.client.IndividualResource;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import api.support.APITests;
import api.support.builders.CheckOutByBarcodeRequestBuilder;
import api.support.builders.NoticeConfigurationBuilder;
import api.support.builders.NoticePolicyBuilder;
import api.support.fixtures.NoticeMatchers;
import io.vertx.core.json.JsonObject;

public class ScheduledDueDateNoticesProcessingTests extends APITests {


  private static final String BEFORE_TIMING = "Before";
  private static final String UPON_AT_TIMING = "Upon At";
  private static final String AFTER_TIMING = "After";


  private final UUID beforeTemplateId = UUID.randomUUID();
  private final Period beforePeriod = Period.days(2);
  private final Period beforeRecurringPeriod = Period.hours(6);

  private final UUID uponAtTemplateId = UUID.randomUUID();

  private final UUID afterTemplateId = UUID.randomUUID();
  private final Period afterPeriod = Period.days(3);
  private final Period afterRecurringPeriod = Period.hours(4);

  private final DateTime loanDate = new DateTime(2018, 3, 18, 11, 43, 54, DateTimeZone.UTC);

  private IndividualResource item;
  private IndividualResource borrower;
  private IndividualResource loan;
  private DateTime dueDate;


  @Before
  public void beforeEach()
    throws MalformedURLException,
    InterruptedException,
    TimeoutException,
    ExecutionException {

    setUpNoticePolicy();

    item = itemsFixture.basedUponSmallAngryPlanet();
    borrower = usersFixture.steve();

    loan = loansFixture.checkOutByBarcode(
      new CheckOutByBarcodeRequestBuilder()
        .forItem(item)
        .to(borrower)
        .on(loanDate)
        .at(UUID.randomUUID()));

    dueDate = new DateTime(loan.getJson().getString("dueDate"));

    assertSetUpIsCorrect();
  }


  private void setUpNoticePolicy()
    throws MalformedURLException,
    InterruptedException,
    TimeoutException,
    ExecutionException {

    JsonObject beforeDueDateNoticeConfiguration = new NoticeConfigurationBuilder()
      .withTemplateId(beforeTemplateId)
      .withDueDateEvent()
      .withBeforeTiming(beforePeriod)
      .recurring(beforeRecurringPeriod)
      .sendInRealTime(true)
      .create();
    JsonObject uponAtDueDateNoticeConfiguration = new NoticeConfigurationBuilder()
      .withTemplateId(uponAtTemplateId)
      .withDueDateEvent()
      .withUponAtTiming()
      .sendInRealTime(true)
      .create();
    JsonObject afterDueDateNoticeConfiguration = new NoticeConfigurationBuilder()
      .withTemplateId(afterTemplateId)
      .withDueDateEvent()
      .withAfterTiming(afterPeriod)
      .recurring(afterRecurringPeriod)
      .sendInRealTime(true)
      .create();

    NoticePolicyBuilder noticePolicy = new NoticePolicyBuilder()
      .withName("Policy with due date notices")
      .withLoanNotices(Arrays.asList(
        beforeDueDateNoticeConfiguration,
        uponAtDueDateNoticeConfiguration,
        afterDueDateNoticeConfiguration));
    useLoanPolicyAsFallback(
      loanPoliciesFixture.canCirculateRolling().getId(),
      requestPoliciesFixture.allowAllRequestPolicy().getId(),
      noticePoliciesFixture.create(noticePolicy).getId());
  }

  private void assertSetUpIsCorrect()
    throws MalformedURLException,
    InterruptedException,
    TimeoutException,
    ExecutionException {

    Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(scheduledNoticesClient::getAll, hasSize(3));

    checkScheduledNotices(
      dueDate.minus(beforePeriod.timePeriod()),
      dueDate,
      dueDate.plus(afterPeriod.timePeriod()));
  }

  private void checkScheduledNotices(
    DateTime beforeNoticeNextRunTime,
    DateTime uponAtNoticeNextRunTime,
    DateTime afterNoticeNextRunTime)
    throws MalformedURLException,
    InterruptedException,
    TimeoutException,
    ExecutionException {

    int numberOfExpectedScheduledNotices = 0;
    numberOfExpectedScheduledNotices += beforeNoticeNextRunTime != null ? 1 : 0;
    numberOfExpectedScheduledNotices += uponAtNoticeNextRunTime != null ? 1 : 0;
    numberOfExpectedScheduledNotices += afterNoticeNextRunTime != null ? 1 : 0;

    List<JsonObject> scheduledNotices = scheduledNoticesClient.getAll();

    assertThat(scheduledNotices, hasSize(numberOfExpectedScheduledNotices));
    if (beforeNoticeNextRunTime != null) {
      assertThat(scheduledNotices, hasItems(
        hasScheduledLoanNotice(
          loan.getId(), beforeNoticeNextRunTime,
          BEFORE_TIMING, beforeTemplateId,
          beforeRecurringPeriod, true)));
    }
    if (uponAtNoticeNextRunTime != null) {
      assertThat(scheduledNotices, hasItems(
        hasScheduledLoanNotice(
          loan.getId(), uponAtNoticeNextRunTime,
          UPON_AT_TIMING, uponAtTemplateId,
          null, true)));
    }
    if (afterNoticeNextRunTime != null) {
      assertThat(scheduledNotices, hasItems(
        hasScheduledLoanNotice(
          loan.getId(), afterNoticeNextRunTime,
          AFTER_TIMING, afterTemplateId,
          afterRecurringPeriod, true)));
    }
  }

  @SuppressWarnings("unchecked")
  private void checkSentNotices(UUID... expectedTemplateIds)
    throws MalformedURLException,
    InterruptedException,
    TimeoutException,
    ExecutionException {

    Map<String, Matcher<String>> noticeContextMatchers = new HashMap<>();
    noticeContextMatchers.putAll(NoticeMatchers.getUserContextMatchers(borrower));
    noticeContextMatchers.putAll(NoticeMatchers.getItemContextMatchers(item));
    noticeContextMatchers.putAll(NoticeMatchers.getLoanContextMatchers(loan, 0));
    noticeContextMatchers.putAll(NoticeMatchers.getLoanPolicyContextMatchers(
      loanPoliciesFixture.canCirculateRolling(), 0));

    Matcher[] matchers = Stream.of(expectedTemplateIds)
      .map(templateId -> hasEmailNoticeProperties(borrower.getId(), templateId, noticeContextMatchers))
      .toArray(Matcher[]::new);

    List<JsonObject> sentNotices = patronNoticesClient.getAll();
    assertThat(sentNotices, hasSize(expectedTemplateIds.length));
    assertThat(sentNotices, hasItems(matchers));
  }

  @Test
  public void beforeNoticeShouldBeSentAndItsNextRunTimeShouldBeUpdated()
    throws MalformedURLException,
    InterruptedException,
    TimeoutException,
    ExecutionException {

    DateTime beforeDueDateTime = dueDate.minus(beforePeriod.timePeriod()).plusSeconds(1);
    scheduledNoticeProcessingTimerClient.runNoticesProcessing(beforeDueDateTime);

    checkSentNotices(beforeTemplateId);

    DateTime expectedNewRunTimeForBeforeNotice = dueDate
      .minus(beforePeriod.timePeriod())
      .plus(beforeRecurringPeriod.timePeriod());

    checkScheduledNotices(
      expectedNewRunTimeForBeforeNotice,
      dueDate,
      dueDate.plus(afterPeriod.timePeriod()));
  }

  @Test
  public void beforeNoticeShouldBeSendAndDeletedWhenItsNextRunTimeIsAfterDueDate()
    throws MalformedURLException,
    InterruptedException,
    TimeoutException,
    ExecutionException {

    DateTime justBeforeDueDateTime = dueDate.minusSeconds(1);
    scheduledNoticeProcessingTimerClient.runNoticesProcessing(justBeforeDueDateTime);

    checkSentNotices(beforeTemplateId);

    checkScheduledNotices(
      null,
      dueDate,
      dueDate.plus(afterPeriod.timePeriod()));
  }

  @Test
  public void beforeAndUponAtNoticesShouldBeSentWhenProcessingJustAfterDueDate()
    throws MalformedURLException,
    InterruptedException,
    TimeoutException,
    ExecutionException {

    DateTime justAfterDueDateTime = dueDate.plusSeconds(1);
    scheduledNoticeProcessingTimerClient.runNoticesProcessing(justAfterDueDateTime);

    checkSentNotices(beforeTemplateId, uponAtTemplateId);

    checkScheduledNotices(
      null,
      null,
      dueDate.plus(afterPeriod.timePeriod()));
  }

  @Test
  public void afterRecurringNoticeShouldBeSentSeveralTimesBeforeLoanIsClosed()
    throws MalformedURLException,
    InterruptedException,
    TimeoutException,
    ExecutionException {

    DateTime justAfterDueDateTime = dueDate.plusSeconds(1);
    scheduledNoticeProcessingTimerClient.runNoticesProcessing(justAfterDueDateTime);
    //Clear all sent notices before actual test
    patronNoticesClient.deleteAll();

    DateTime afterNoticeRunTime = dueDate.plus(afterPeriod.timePeriod()).plusSeconds(1);
    scheduledNoticeProcessingTimerClient.runNoticesProcessing(afterNoticeRunTime);

    DateTime afterNoticeExpectedRunTime = dueDate
      .plus(afterPeriod.timePeriod())
      .plus(afterRecurringPeriod.timePeriod());

    checkScheduledNotices(
      null,
      null,
      afterNoticeExpectedRunTime);

    //Run again to send recurring notice
    scheduledNoticeProcessingTimerClient.runNoticesProcessing(
      afterNoticeExpectedRunTime.plusSeconds(1));

    checkSentNotices(afterTemplateId, afterTemplateId);

    DateTime secondRecurringRunTime =
      afterNoticeExpectedRunTime.plus(afterRecurringPeriod.timePeriod());

    checkScheduledNotices(
      null,
      null,
      secondRecurringRunTime);

    loansFixture.checkInByBarcode(item);
    //Clear sent notices again
    patronNoticesClient.deleteAll();

    //Run after loan is closed
    scheduledNoticeProcessingTimerClient.runNoticesProcessing(
      secondRecurringRunTime.plusSeconds(1));

    checkSentNotices();
    checkScheduledNotices(null, null, null);
  }

}
