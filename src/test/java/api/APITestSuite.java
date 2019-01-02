package api;

import static org.folio.circulation.support.JsonPropertyWriter.write;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.folio.circulation.Launcher;
import org.folio.circulation.domain.policy.Period;
import org.folio.circulation.support.VertxAssistant;
import org.folio.circulation.support.http.client.OkapiHttpClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import api.support.builders.FixedDueDateSchedule;
import api.support.builders.FixedDueDateSchedulesBuilder;
import api.support.builders.LoanPolicyBuilder;
import api.support.builders.ServicePointBuilder;
import api.support.examples.LocationExamples;
import api.support.fakes.FakeOkapi;
import api.support.fakes.FakeStorageModule;
import api.support.http.ResourceClient;
import api.support.http.URLHelper;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class APITestSuite {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String TENANT_ID = "test_tenant";
  public static final String USER_ID = "79ff2a8b-d9c3-5b39-ad4a-0a84025ab085";
  public static final String TOKEN = "eyJhbGciOiJIUzUxMiJ9eyJzdWIiOiJhZG1pbiIsInVzZXJfaWQiOiI3OWZmMmE4Yi1kOWMzLTViMzktYWQ0YS0wYTg0MDI1YWIwODUiLCJ0ZW5hbnQiOiJ0ZXN0X3RlbmFudCJ9BShwfHcNClt5ZXJ8ImQTMQtAM1sQEnhsfWNmXGsYVDpuaDN3RVQ9";
  private static final String REQUEST_ID = createFakeRequestId();

  private static VertxAssistant vertxAssistant;
  private static Launcher launcher;
  private static int port;

  private static String fakeOkapiDeploymentId;
  private static Boolean useOkapiForStorage;
  private static Boolean useOkapiForInitialRequests;
  private static UUID booksInstanceTypeId;
  private static UUID regularGroupId;
  private static UUID alternateGroupId;
  private static UUID workAddressTypeId;
  private static UUID personalContributorTypeId;

  public static UUID nottinghamUniversityInstitution;
  public static UUID jubileeCampus;
  public static UUID djanoglyLibrary;
  public static UUID businessLibrary;
  public static UUID thirdFloorLocationId;
  public static UUID mezzanineDisplayCaseLocationId;
  public static UUID secondFloorEconomicsLocationId;
  public static UUID fakeServicePointId;

  private static UUID canCirculateRollingLoanPolicyId;
  private static UUID canCirculateFixedLoanPolicyId;
  private static UUID exampleFixedDueDateSchedulesId;

  private static UUID courseReservesCancellationReasonId;
  private static UUID patronRequestCancellationReasonId;

  public static int circulationModulePort() {
    return port;
  }

  public static URL circulationModuleUrl(String path) {
    try {
      if (useOkapiForInitialRequests) {
        return URLHelper.joinPath(okapiUrl(), path);
      } else {
        return new URL("http", "localhost", port, path);
      }
    } catch (MalformedURLException ex) {
      return null;
    }
  }

  public static URL viaOkapiModuleUrl(String path) {
    try {
      return URLHelper.joinPath(okapiUrl(), path);
    } catch (MalformedURLException ex) {
      return null;
    }
  }

  public static OkapiHttpClient createClient(
    Consumer<Throwable> exceptionHandler) {

    return new OkapiHttpClient(
      vertxAssistant.createUsingVertx(Vertx::createHttpClient),
      okapiUrl(), TENANT_ID, TOKEN, USER_ID, REQUEST_ID, exceptionHandler);
  }

  public static OkapiHttpClient createClient() {
    return APITestSuite.createClient(exception ->
      log.error("Request failed:", exception));
  }

  public static UUID thirdFloorLocationId() {
    return thirdFloorLocationId;
  }

  public static UUID mezzanineDisplayCaseLocationId() {
    return mezzanineDisplayCaseLocationId;
  }

  public static UUID secondFloorEconomicsLocationId() {
    return secondFloorEconomicsLocationId;
  }

  public static UUID booksInstanceTypeId() {
    return booksInstanceTypeId;
  }

  public static UUID personalContributorNameTypeId() {
    return personalContributorTypeId;
  }

  public static UUID regularGroupId() {
    return regularGroupId;
  }

  public static UUID alternateGroupId() {
    return alternateGroupId;
  }

  public static UUID workAddressTypeId() {
    return workAddressTypeId;
  }

  public static UUID canCirculateRollingLoanPolicyId() {
    return canCirculateRollingLoanPolicyId;
  }

  public static UUID canCirculateFixedLoanPolicyId() {
    return canCirculateFixedLoanPolicyId;
  }

  public static UUID courseReservesCancellationReasonId() {
    return courseReservesCancellationReasonId;
  }

  public static UUID nottinghamUniversityInstitution() {
    return nottinghamUniversityInstitution;
  }

  public static UUID jubileeCampus() {
    return jubileeCampus;
  }

  public static UUID djanoglyLibrary() {
    return djanoglyLibrary;
  }

  public static UUID businessLibrary() {
    return businessLibrary;
  }

  public static UUID fakeServicePoint() {
    return fakeServicePointId;
  }

  public static void createCommonRecords()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    createContributorNameTypes();
    createInstanceTypes();

    createAddressTypes();
    createGroups();

    createLoanPolicies();
    createCancellationReasons();
  }

  public static void deployVerticles()
    throws InterruptedException,
    ExecutionException,
    TimeoutException {

    final CompletableFuture<String> fakeStorageModuleDeployed = deployFakeStorageModules();

    final CompletableFuture<Void> circulationModuleStarted = launcher.start(port);

    fakeStorageModuleDeployed.thenAccept(result -> fakeOkapiDeploymentId = result);

    CompletableFuture.allOf(circulationModuleStarted, fakeStorageModuleDeployed)
      .get(10, TimeUnit.SECONDS);
  }

  private static CompletableFuture<String> deployFakeStorageModules() {
    useOkapiForStorage = Boolean.parseBoolean(
      System.getProperty("use.okapi.storage.requests", "false"));

    useOkapiForInitialRequests = Boolean.parseBoolean(
      System.getProperty("use.okapi.initial.requests", "false"));

    port = 9605;
    vertxAssistant = new VertxAssistant();
    launcher = new Launcher(vertxAssistant);

    vertxAssistant.start();

    final CompletableFuture<String> fakeStorageModuleDeployed;

    if (!useOkapiForStorage) {
      fakeStorageModuleDeployed = vertxAssistant.deployVerticle(FakeOkapi.class);
    } else {
      fakeStorageModuleDeployed = CompletableFuture.completedFuture(null);
    }
    return fakeStorageModuleDeployed;
  }

  public static void undeployVerticles()
    throws InterruptedException,
    ExecutionException,
    TimeoutException {

    CompletableFuture<Void> circulationModuleUndeployed = launcher.undeploy();

    final CompletableFuture<Void> fakeOkapiUndeployed;

    if (!useOkapiForStorage) {
      outputCQLQueryRequestsPerformedAgainstFakes();

      fakeOkapiUndeployed = vertxAssistant.undeployVerticle(fakeOkapiDeploymentId);
    } else {
      fakeOkapiUndeployed = CompletableFuture.completedFuture(null);
    }

    circulationModuleUndeployed.get(10, TimeUnit.SECONDS);
    fakeOkapiUndeployed.get(10, TimeUnit.SECONDS);

    CompletableFuture<Void> stopped = vertxAssistant.stop();

    stopped.get(5, TimeUnit.SECONDS);
  }

  private static void outputCQLQueryRequestsPerformedAgainstFakes() {
    final String sortedRequests = FakeStorageModule.getQueries()
      .sorted()
      .collect(Collectors.joining("\n"));

    log.info("Queries performed: {}", sortedRequests);
  }

  public static void deleteAllRecords()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    OkapiHttpClient client = APITestSuite.createClient(exception ->
      log.error("Requests to delete all for clean up failed:", exception));

    ResourceClient.forRequests(client).deleteAll();
    ResourceClient.forLoans(client).deleteAll();

    ResourceClient.forItems(client).deleteAll();
    ResourceClient.forHoldings(client).deleteAll();
    ResourceClient.forInstances(client).deleteAll();

    ResourceClient.forLoanPolicies(client).deleteAllIndividually();
    ResourceClient.forFixedDueDateSchedules(client).deleteAllIndividually();

    ResourceClient.forMaterialTypes(client).deleteAllIndividually();
    ResourceClient.forLoanTypes(client).deleteAllIndividually();

    ResourceClient.forUsers(client).deleteAllIndividually();

    ResourceClient.forPatronGroups(client).deleteAllIndividually();
    ResourceClient.forAddressTypes(client).deleteAllIndividually();

    ResourceClient.forMaterialTypes(client).deleteAllIndividually();
    ResourceClient.forLoanTypes(client).deleteAllIndividually();
    ResourceClient.forLocations(client).deleteAllIndividually();
    ResourceClient.forServicePoints(client).deleteAllIndividually();
    ResourceClient.forContributorNameTypes(client).deleteAllIndividually();
    ResourceClient.forInstanceTypes(client).deleteAllIndividually();
    ResourceClient.forCancellationReasons(client).deleteAllIndividually();
  }

  public static URL okapiUrl() {
    try {
      if (useOkapiForStorage) {
        return new URL("http://localhost:9130");
      } else {
        return new URL(FakeOkapi.getAddress());
      }
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException("Invalid Okapi URL configured for tests");
    }
  }

  private static void createGroups()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {
    ResourceClient groupsClient = ResourceClient.forPatronGroups(createClient());

    groupsClient.deleteAllIndividually();

    regularGroupId = groupsClient.create(new JsonObject()
      .put("group", "Regular Group")
      .put("desc", "Regular group")
    ).getId();

    alternateGroupId = groupsClient.create(new JsonObject()
      .put("group", "Alternative Group")
      .put("desc", "Regular group")
    ).getId();
  }

  public static void deleteGroups()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient groupsClient = ResourceClient.forPatronGroups(createClient());
    groupsClient.delete(regularGroupId);
    groupsClient.delete(alternateGroupId);
  }

  private static void createAddressTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {
    ResourceClient addressTypesClient = ResourceClient.forAddressTypes(createClient());

    addressTypesClient.deleteAllIndividually();

    workAddressTypeId = addressTypesClient.create(new JsonObject()
      .put("addressType", "Work")
      .put("desc", "Work address type"))
      .getId();
  }

  public static void deleteAddressTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient groupsClient = ResourceClient.forAddressTypes(createClient());
    groupsClient.delete(workAddressTypeId);
  }

  private static void createContributorNameTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {
    personalContributorTypeId = createReferenceRecord(
      ResourceClient.forContributorNameTypes(createClient()), "Personal name");
  }

  public static void deleteContributorTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient contributorTypesClient = ResourceClient.forContributorNameTypes(createClient());
    contributorTypesClient.delete(personalContributorTypeId);
  }

  private static void createInstanceTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    booksInstanceTypeId = createReferenceRecord(
      ResourceClient.forInstanceTypes(createClient()), "Books", "BO", "tests");
  }

  public static void deleteInstanceTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient instanceTypesClient = ResourceClient.forInstanceTypes(createClient());

    instanceTypesClient.delete(booksInstanceTypeId());
  }

  private static void createLoanPolicies()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    final OkapiHttpClient client = createClient();

    ResourceClient loanPoliciesClient = ResourceClient.forLoanPolicies(client);

    LoanPolicyBuilder canCirculateRollingLoanPolicy = new LoanPolicyBuilder()
      .withName("Can Circulate Rolling")
      .withDescription("Can circulate item")
      .rolling(Period.weeks(3))
      .unlimitedRenewals()
      .renewFromSystemDate();

    canCirculateRollingLoanPolicyId = loanPoliciesClient.create(
      canCirculateRollingLoanPolicy).getId();

    ResourceClient fixedDueDateSchedulesClient = ResourceClient.forFixedDueDateSchedules(client);

    FixedDueDateSchedulesBuilder fixedDueDateSchedule =
      new FixedDueDateSchedulesBuilder()
        .withName("Example Fixed Due Date Schedule")
        .withDescription("Example Fixed Due Date Schedule")
        .addSchedule(new FixedDueDateSchedule(
          new DateTime(2018, 1, 1, 0, 0, 0, DateTimeZone.UTC),
          new DateTime(2018, 12, 31, 23, 59, 59, DateTimeZone.UTC),
          new DateTime(2018, 12, 31, 23, 59, 59, DateTimeZone.UTC)
        ));

    exampleFixedDueDateSchedulesId = fixedDueDateSchedulesClient.create(
      fixedDueDateSchedule).getId();

    LoanPolicyBuilder canCirculateFixedLoanPolicy = new LoanPolicyBuilder()
      .withName("Can Circulate Fixed")
      .withDescription("Can circulate item")
      .fixed(exampleFixedDueDateSchedulesId);

    canCirculateFixedLoanPolicyId = loanPoliciesClient.create(
      canCirculateFixedLoanPolicy).getId();

    log.info("Rolling loan policy {}", canCirculateRollingLoanPolicyId);
    log.info("Fixed loan policy {}", canCirculateFixedLoanPolicyId);
  }

  public static void deleteLoanPolicies()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient loanPoliciesClient = ResourceClient.forLoanPolicies(createClient());

    loanPoliciesClient.delete(canCirculateRollingLoanPolicyId());
    loanPoliciesClient.delete(canCirculateFixedLoanPolicyId());

    ResourceClient fixedDueDateSchedulesClient = ResourceClient.forFixedDueDateSchedules(createClient());

    fixedDueDateSchedulesClient.delete(exampleFixedDueDateSchedulesId);
  }

  static void setLoanRules(String rules)
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient client = ResourceClient.forLoanRules(createClient());
    JsonObject json = new JsonObject().put("loanRulesAsTextFile", rules);
    client.replace(null, json);
  }

  public static UUID createReferenceRecord(
    ResourceClient client,
    JsonObject record)
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    List<JsonObject> existingRecords = client.getAll();

    String name = record.getString("name");

    if(name == null) {
      throw new IllegalArgumentException("Reference records must have a name");
    }

    if(existsInList(existingRecords, name)) {
      return client.create(record).getId();
    }
    else {
      return findFirstByName(existingRecords, name);
    }
  }

  public static UUID createReferenceRecord(
    ResourceClient client,
    String name)
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    return createReferenceRecord(client, name, null, null);
  }

  public static UUID createReferenceRecord(
    ResourceClient client,
    String name,
    String code,
    String source)
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    final JsonObject referenceRecord = new JsonObject();

    write(referenceRecord, "name", name);
    write(referenceRecord, "code", code);
    write(referenceRecord, "source", source);

    return createReferenceRecord(client, referenceRecord);
  }

  private static void createCancellationReasons()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    courseReservesCancellationReasonId = createReferenceRecord(
        ResourceClient.forCancellationReasons(createClient()),
        new JsonObject()
            .put("name", "Course Reserves")
            .put("description", "Item Needed for Course Reserves")
    );

    patronRequestCancellationReasonId = createReferenceRecord(
        ResourceClient.forCancellationReasons(createClient()),
        new JsonObject()
            .put("name", "Patron Request")
            .put("description", "Item cancelled at Patron request")
    );
  }

  public static void deleteCancellationReasons()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient cancellationReasonClient =
        ResourceClient.forCancellationReasons(createClient());

    cancellationReasonClient.delete(courseReservesCancellationReasonId);
    cancellationReasonClient.delete(patronRequestCancellationReasonId);
  }

  private static UUID findFirstByName(List<JsonObject> existingRecords, String name) {
    return UUID.fromString(existingRecords.stream()
      .filter(record -> record.getString("name").equals(name))
      .findFirst()
      .get()
      .getString("id"));
  }

  private static boolean existsInList(List<JsonObject> existingRecords, String name) {
    return existingRecords.stream()
      .noneMatch(materialType -> materialType.getString("name").equals(name));
  }

  private static String createFakeRequestId() {
    return String.format("%s/fake-context", new Random().nextInt(999999));
  }
}
