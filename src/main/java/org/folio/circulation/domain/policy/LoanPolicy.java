package org.folio.circulation.domain.policy;

import static org.folio.circulation.support.HttpResult.failed;
import static org.folio.circulation.support.JsonPropertyFetcher.getBooleanProperty;
import static org.folio.circulation.support.JsonPropertyFetcher.getIntegerProperty;
import static org.folio.circulation.support.JsonPropertyFetcher.getNestedIntegerProperty;
import static org.folio.circulation.support.JsonPropertyFetcher.getNestedStringProperty;
import static org.folio.circulation.support.JsonPropertyFetcher.getProperty;
import static org.folio.circulation.support.ValidationErrorFailure.failedResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.folio.circulation.domain.Loan;
import org.folio.circulation.support.HttpResult;
import org.folio.circulation.support.ServerErrorFailure;
import org.folio.circulation.support.ValidationErrorFailure;
import org.folio.circulation.support.http.server.ValidationError;
import org.joda.time.DateTime;

import io.vertx.core.json.JsonObject;

public class LoanPolicy {
  private final JsonObject representation;
  private final FixedDueDateSchedules fixedDueDateSchedules;
  private final FixedDueDateSchedules alternateRenewalFixedDueDateSchedules;

  private LoanPolicy(JsonObject representation) {
    this(representation,
      new NoFixedDueDateSchedules(),
      new NoFixedDueDateSchedules());
  }

  LoanPolicy(
    JsonObject representation,
    FixedDueDateSchedules fixedDueDateSchedules,
    FixedDueDateSchedules alternateRenewalFixedDueDateSchedules) {

    this.representation = representation;
    this.fixedDueDateSchedules = fixedDueDateSchedules;
    this.alternateRenewalFixedDueDateSchedules = alternateRenewalFixedDueDateSchedules;
  }

  static LoanPolicy from(JsonObject representation) {
    return new LoanPolicy(representation);
  }

  //TODO: make this have similar signature to renew
  public HttpResult<DateTime> calculateInitialDueDate(Loan loan) {
    return determineStrategy(false, null).calculateDueDate(loan);
  }

  public HttpResult<Loan> renew(Loan loan, DateTime systemDate) {
    //TODO: Create HttpResult wrapper that traps exceptions
    try {
      if(isNotRenewable()) {
        return failedResult(errorForPolicy("items with this loan policy cannot be renewed"));
      }

      final HttpResult<DateTime> proposedDueDateResult =
        determineStrategy(true, systemDate).calculateDueDate(loan);

      List<ValidationError> errors = new ArrayList<>();

      //TODO: Need a more elegent way of combining validation errors
      if(proposedDueDateResult.failed()) {
        if (proposedDueDateResult.cause() instanceof ValidationErrorFailure) {
          ValidationErrorFailure failureCause =
            (ValidationErrorFailure) proposedDueDateResult.cause();

          errors.addAll(failureCause.getErrors());
        }
      }
      else {
        errorWhenEarlierOrSameDueDate(loan, proposedDueDateResult.value(), errors);
      }

      errorWhenReachedRenewalLimit(loan, errors);

      if(errors.isEmpty()) {
        return proposedDueDateResult.map(dueDate -> loan.renew(dueDate, getId()));
      }
      else {
        return HttpResult.failed(new ValidationErrorFailure(errors));
      }
    }
    catch(Exception e) {
      return failed(new ServerErrorFailure(e));
    }
  }

  private ValidationError errorForPolicy(String reason) {
    HashMap<String, String> parameters = new HashMap<>();
    parameters.put("loanPolicyId", getId());
    parameters.put("loanPolicyName", getName());

    return new ValidationError(reason, parameters);
  }

  private boolean isNotRenewable() {
    return !getBooleanProperty(representation, "renewable");
  }

  private void errorWhenReachedRenewalLimit(Loan loan, List<ValidationError> errors) {
    if(!unlimitedRenewals() && reachedNumberOfRenewalsLimit(loan)) {
      errors.add(errorForPolicy("loan has reached its maximum number of renewals"));
    }
  }

  private void errorWhenEarlierOrSameDueDate(
    Loan loan,
    DateTime proposedDueDate,
    List<ValidationError> errors) {

    if(isSameOrBefore(loan, proposedDueDate)) {
      errors.add(errorForPolicy(
        "renewal at this time would not change the due date"));
    }
  }

  private boolean isSameOrBefore(Loan loan, DateTime proposedDueDate) {
    return proposedDueDate.isEqual(loan.getDueDate())
      || proposedDueDate.isBefore(loan.getDueDate());
  }

  private boolean reachedNumberOfRenewalsLimit(Loan loan) {
    return loan.getRenewalCount() >= getRenewalLimit();
  }

  private boolean unlimitedRenewals() {
    return getBooleanProperty(getRenewalsPolicy(), "unlimited");
  }

  private Integer getRenewalLimit() {
    return getIntegerProperty(getRenewalsPolicy(), "numberAllowed", 0);
  }

  private DueDateStrategy determineStrategy(boolean isRenewal, DateTime systemDate) {
    final JsonObject loansPolicy = getLoansPolicy();
    final JsonObject renewalsPolicy = getRenewalsPolicy();

    //TODO: Temporary until have better logic for missing loans policy
    if(loansPolicy == null) {
      return new UnknownDueDateStrategy(getId(), getName(), "", isRenewal,
        this::errorForPolicy);
    }

    if(isRolling(loansPolicy)) {
      if(isRenewal) {
        return new RollingRenewalDueDateStrategy(getId(), getName(),
          systemDate, getRenewFrom(), getRenewalPeriod(loansPolicy, renewalsPolicy),
          getRenewalDueDateLimitSchedules(), this::errorForPolicy);
      }
      else {
        return new RollingCheckOutDueDateStrategy(getId(), getName(),
          getPeriod(loansPolicy), fixedDueDateSchedules, this::errorForPolicy);
      }
    }
    else if(isFixed(loansPolicy)) {
      if(isRenewal) {
        return new FixedScheduleRenewalDueDateStrategy(getId(), getName(),
          getRenewalFixedDueDateSchedules(), systemDate, this::errorForPolicy);
      }
      else {
        return new FixedScheduleCheckOutDueDateStrategy(getId(), getName(),
          fixedDueDateSchedules, this::errorForPolicy);
      }
    }
    else {
      return new UnknownDueDateStrategy(getId(), getName(),
        getProfileId(loansPolicy), isRenewal, this::errorForPolicy);
    }
  }

  private JsonObject getLoansPolicy() {
    return representation.getJsonObject("loansPolicy");
  }

  private JsonObject getRenewalsPolicy() {
    return representation.getJsonObject("renewalsPolicy");
  }

  private FixedDueDateSchedules getRenewalDueDateLimitSchedules() {
    if(useDifferentPeriod()) {
      if(Objects.isNull(alternateRenewalFixedDueDateSchedules)
        || alternateRenewalFixedDueDateSchedules instanceof NoFixedDueDateSchedules)
        return fixedDueDateSchedules;
      else {
        return alternateRenewalFixedDueDateSchedules;
      }
    }
    else {
      return fixedDueDateSchedules;
    }
  }

  private Period getRenewalPeriod(
    JsonObject loansPolicy,
    JsonObject renewalsPolicy) {

    return useDifferentPeriod()
      ? getPeriod(renewalsPolicy)
      : getPeriod(loansPolicy);
  }

  private Period getPeriod(JsonObject policy) {
    String interval = getNestedStringProperty(policy, "period", "intervalId");
    Integer duration = getNestedIntegerProperty(policy, "period", "duration");
    return Period.from(duration, interval);
  }

  private Boolean useDifferentPeriod() {
    return getBooleanProperty(getRenewalsPolicy(), "differentPeriod");
  }

  private String getRenewFrom() {
    return getProperty(getRenewalsPolicy(), "renewFromId");
  }

  private FixedDueDateSchedules getRenewalFixedDueDateSchedules() {
    return useDifferentPeriod()
      ? alternateRenewalFixedDueDateSchedules
      : fixedDueDateSchedules;
  }

  private String getProfileId(JsonObject loansPolicy) {
    return loansPolicy.getString("profileId");
  }

  private String getName() {
    return representation.getString("name");
  }

  private boolean isFixed(JsonObject loansPolicy) {
    return isProfile(loansPolicy, "Fixed");
  }

  private boolean isRolling(JsonObject loansPolicy) {
    return isProfile(loansPolicy, "Rolling");
  }

  private boolean isProfile(JsonObject loansPolicy, String profileId) {
    return StringUtils.equalsIgnoreCase(getProfileId(loansPolicy), profileId);
  }

  LoanPolicy withDueDateSchedules(FixedDueDateSchedules loanSchedules) {
    return new LoanPolicy(representation, loanSchedules,
      alternateRenewalFixedDueDateSchedules);
  }

  //TODO: potentially remove this, when builder can create class or JSON representation
  LoanPolicy withDueDateSchedules(JsonObject fixedDueDateSchedules) {
    return withDueDateSchedules(FixedDueDateSchedules.from(fixedDueDateSchedules));
  }

  LoanPolicy withAlternateRenewalSchedules(FixedDueDateSchedules renewalSchedules) {
    return new LoanPolicy(representation, fixedDueDateSchedules, renewalSchedules);
  }

  //TODO: potentially remove this, when builder can create class or JSON representation
  LoanPolicy withAlternateRenewalSchedules(JsonObject renewalSchedules) {
    return withAlternateRenewalSchedules(FixedDueDateSchedules.from(renewalSchedules));
  }

  public String getId() {
    return representation.getString("id");
  }

  String getLoansFixedDueDateScheduleId() {
    return getProperty(getLoansPolicy(), "fixedDueDateScheduleId");
  }

  String getAlternateRenewalsFixedDueDateScheduleId() {
    return getProperty(getRenewalsPolicy(), "alternateFixedDueDateScheduleId");
  }
}
