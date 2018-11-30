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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import api.loans.CheckInByBarcodeTests;
import api.loans.CheckInByReplacingLoanTests;
import api.loans.CheckOutByBarcodeTests;
import api.loans.LoanAPILocationTests;
import api.loans.LoanAPIPolicyTests;
import api.loans.LoanAPIProxyTests;
import api.loans.LoanAPIRelatedRecordsTests;
import api.loans.LoanAPITests;
import api.loans.LoanAPITitleTests;
import api.loans.RenewByBarcodeTests;
import api.loans.RenewByIdTests;
import api.requests.RequestsAPICreateMultipleRequestsTests;
import api.requests.RequestsAPICreationTests;
import api.requests.RequestsAPIDeletionTests;
import api.requests.RequestsAPILoanHistoryTests;
import api.requests.RequestsAPILoanRenewalTests;
import api.requests.RequestsAPILocationTests;
import api.requests.RequestsAPIProxyTests;
import api.requests.RequestsAPIRelatedRecordsTests;
import api.requests.RequestsAPIRetrievalTests;
import api.requests.RequestsAPIStatusChangeTests;
import api.requests.RequestsAPITitleTests;
import api.requests.RequestsAPIUpdatingTests;
import api.requests.scenarios.ClosedRequestTests;
import api.requests.scenarios.MultipleHoldShelfRequestsTests;
import api.requests.scenarios.MultipleMixedFulfilmentRequestsTests;
import api.requests.scenarios.MultipleOutOfOrderRequestsTests;
import api.requests.scenarios.RequestQueueTests;
import api.requests.scenarios.RequestsForDifferentItemsTests;
import api.requests.scenarios.SingleClosedRequestTests;
import api.requests.scenarios.SingleOpenDeliveryRequestTests;
import api.requests.scenarios.SingleOpenHoldShelfRequestTests;
import api.support.builders.FixedDueDateSchedule;
import api.support.builders.FixedDueDateSchedulesBuilder;
import api.support.builders.LoanPolicyBuilder;
import api.support.builders.UserBuilder;
import api.support.fakes.FakeOkapi;
import api.support.fakes.FakeStorageModule;
import api.support.http.ResourceClient;
import api.support.http.URLHelper;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


@RunWith(Suite.class)

@Suite.SuiteClasses({
  CheckOutByBarcodeTests.class,
  RenewByBarcodeTests.class,
  RenewByIdTests.class,
  CheckInByBarcodeTests.class,
  CheckInByReplacingLoanTests.class,
  LoanAPITests.class,
  LoanAPILocationTests.class,
  LoanAPITitleTests.class,
  LoanAPIRelatedRecordsTests.class,
  LoanAPIPolicyTests.class,
  LoanRulesAPITests.class,
  LoanAPIProxyTests.class,
  LoanRulesEngineAPITests.class,
  RequestsAPICreationTests.class,
  RequestsAPICreateMultipleRequestsTests.class,
  ClosedRequestTests.class,
  RequestsAPIDeletionTests.class,
  RequestsAPIRetrievalTests.class,
  RequestsAPIUpdatingTests.class,
  RequestsAPIStatusChangeTests.class,
  RequestsAPILoanRenewalTests.class,
  RequestsAPILoanHistoryTests.class,
  RequestsAPILocationTests.class,
  RequestsAPITitleTests.class,
  RequestsAPIProxyTests.class,
  RequestsAPIRelatedRecordsTests.class,
  SingleOpenHoldShelfRequestTests.class,
  SingleOpenDeliveryRequestTests.class,
  SingleClosedRequestTests.class,
  MultipleHoldShelfRequestsTests.class,
  MultipleOutOfOrderRequestsTests.class,
  MultipleMixedFulfilmentRequestsTests.class,
  RequestsForDifferentItemsTests.class,
  RequestQueueTests.class,
})
public class APITestSuite {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String TENANT_ID = "test_tenant";
  public static final String USER_ID = "79ff2a8b-d9c3-5b39-ad4a-0a84025ab085";
  public static final String TOKEN = "eyJhbGciOiJIUzUxMiJ9eyJzdWIiOiJhZG1pbiIsInVzZXJfaWQiOiI3OWZmMmE4Yi1kOWMzLTViMzktYWQ0YS0wYTg0MDI1YWIwODUiLCJ0ZW5hbnQiOiJ0ZXN0X3RlbmFudCJ9BShwfHcNClt5ZXJ8ImQTMQtAM1sQEnhsfWNmXGsYVDpuaDN3RVQ9";
  private static final String REQUEST_ID = createFakeRequestId();

  private static VertxAssistant vertxAssistant;
  private static Launcher launcher;
  private static int port;
  private static boolean initialised;

  private static String circulationModuleDeploymentId;
  private static String fakeOkapiDeploymentId;
  private static Boolean useOkapiForStorage;
  private static Boolean useOkapiForInitialRequests;
  private static UUID bookMaterialTypeId;
  private static UUID videoRecordingMaterialTypeId;
  private static UUID canCirculateLoanTypeId;
  private static UUID readingRoomLoanTypeId;
  private static UUID booksInstanceTypeId;
  private static UUID regularGroupId;
  private static UUID alternateGroupId;
  private static UUID workAddressTypeId;
  private static UUID userId1;
  private static JsonObject userRecord1;
  private static JsonObject userRecord2;
  private static UUID personalContributorTypeId;

  private static UUID nottinghamUniversityInstitution;
  private static UUID jubileeCampus;
  private static UUID djanoglyLibrary;
  private static UUID businessLibrary;
  private static UUID thirdFloorLocationId;
  private static UUID mezzanineDisplayCaseLocationId;
  private static UUID secondFloorEconomicsLocationId;

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

  public static UUID bookMaterialTypeId() {
    return bookMaterialTypeId;
  }

  public static UUID videoRecordingMaterialTypeId() {
    return videoRecordingMaterialTypeId;
  }

  public static UUID canCirculateLoanTypeId() {
    return canCirculateLoanTypeId;
  }

  public static UUID readingRoomLoanTypeId() {
    return readingRoomLoanTypeId;
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

  public static UUID userId() {
    return userId1;
  }

  public static JsonObject userRecord1() {
    return userRecord1;
  }

  public static JsonObject userRecord2() {
    return userRecord2;
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

  public static UUID patronRequestCancellationReasonId() {
    return patronRequestCancellationReasonId;
  }

  @BeforeClass
  public static void before()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

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

    final CompletableFuture<Void> circulationModuleStarted = launcher.start(port);

    fakeStorageModuleDeployed.thenAccept(result -> fakeOkapiDeploymentId = result);

    CompletableFuture.allOf(circulationModuleStarted, fakeStorageModuleDeployed)
      .get(10, TimeUnit.SECONDS);

    createMaterialTypes();
    createLoanTypes();
    createLocations();
    createContributorNameTypes();
    createInstanceTypes();
    createAddressTypes();
    createGroups();
    createUsers();
    createLoanPolicies();
    createCancellationReasons();

    initialised = true;
  }

  @AfterClass
  public static void after()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    initialised = false;

    OkapiHttpClient client = APITestSuite.createClient(exception ->
      log.error("Requests to delete all for clean up failed:", exception));

    ResourceClient.forRequests(client).deleteAll();
    ResourceClient.forLoans(client).deleteAll();

    ResourceClient.forItems(client).deleteAll();
    ResourceClient.forHoldings(client).deleteAll();
    ResourceClient.forInstances(client).deleteAll();

    ResourceClient.forUsers(client).deleteAllIndividually();

    deleteGroups();
    deleteAddressTypes();

    deleteMaterialTypes();
    deleteLoanTypes();
    deleteLocations();
    deleteContributorTypes();
    deleteInstanceTypes();
    deleteLoanPolicies();
    deleteCancellationReasons();

    CompletableFuture<Void> circulationModuleUndeployed = launcher.undeploy();

    final CompletableFuture<Void> fakeOkapiUndeployed;

    if (!useOkapiForStorage) {
      log.info("Queries performed: " + FakeStorageModule.getQueries()
        .sorted()
        .collect(Collectors.joining("\n")));

      fakeOkapiUndeployed = vertxAssistant.undeployVerticle(fakeOkapiDeploymentId);
    } else {
      fakeOkapiUndeployed = CompletableFuture.completedFuture(null);
    }

    circulationModuleUndeployed.get(10, TimeUnit.SECONDS);
    fakeOkapiUndeployed.get(10, TimeUnit.SECONDS);

    CompletableFuture<Void> stopped = vertxAssistant.stop();

    stopped.get(5, TimeUnit.SECONDS);
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

  public static void createUsers()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {
    ResourceClient usersClient = ResourceClient.forUsers(createClient());

    userRecord1 = new UserBuilder()
      .withUsername("bfrederi")
      .withPatronGroupId(regularGroupId)
      .create();

    userId1 = usersClient.create(userRecord1).getId();

    userRecord2 = new UserBuilder()
      .withUsername("lko")
      .withPatronGroupId(alternateGroupId)
      .create();

    usersClient.create(userRecord2).getId();
  }

  public static void createGroups()
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

  private static void deleteGroups()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient groupsClient = ResourceClient.forPatronGroups(createClient());
    groupsClient.delete(regularGroupId);
    groupsClient.delete(alternateGroupId);
  }

  public static void createAddressTypes()
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

  private static void deleteAddressTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient groupsClient = ResourceClient.forAddressTypes(createClient());
    groupsClient.delete(workAddressTypeId);
  }

  private static void createMaterialTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    bookMaterialTypeId = createReferenceRecord(
      ResourceClient.forMaterialTypes(createClient()), "Book");

    videoRecordingMaterialTypeId = createReferenceRecord(
      ResourceClient.forMaterialTypes(createClient()), "Video Recording");
  }

  private static void deleteMaterialTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient materialTypesClient = ResourceClient.forMaterialTypes(createClient());

    materialTypesClient.delete(bookMaterialTypeId);
    materialTypesClient.delete(videoRecordingMaterialTypeId);
  }

  private static void createContributorNameTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {
    personalContributorTypeId = createReferenceRecord(
      ResourceClient.forContributorNameTypes(createClient()), "Personal name");
  }

  private static void deleteContributorTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient contributorTypesClient = ResourceClient.forContributorNameTypes(createClient());
    contributorTypesClient.delete(personalContributorTypeId);
  }

  private static void createLoanTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    canCirculateLoanTypeId = createReferenceRecord(
      ResourceClient.forLoanTypes(createClient()), "Can Circulate");

    readingRoomLoanTypeId = createReferenceRecord(
      ResourceClient.forLoanTypes(createClient()), "Reading Room");
  }

  private static void deleteLoanTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    ResourceClient loanTypesClient = ResourceClient.forLoanTypes(createClient());

    loanTypesClient.delete(canCirculateLoanTypeId);
    loanTypesClient.delete(readingRoomLoanTypeId);
  }

  private static void createLocations()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    final OkapiHttpClient client = createClient();

    ResourceClient institutionsClient = ResourceClient.forInstitutions(client);

    nottinghamUniversityInstitution = createReferenceRecord(institutionsClient,
      "Nottingham University");

    ResourceClient campusesClient = ResourceClient.forCampuses(client);

    jubileeCampus = createReferenceRecord(campusesClient,
      new JsonObject()
        .put("name", "Jubilee Campus")
        .put("institutionId", nottinghamUniversityInstitution.toString()));

    ResourceClient librariesClient = ResourceClient.forLibraries(client);

    djanoglyLibrary = createReferenceRecord(librariesClient,
      new JsonObject()
        .put("name", "Djanogly Learning Resource Centre")
        .put("campusId", jubileeCampus.toString()));

    businessLibrary = createReferenceRecord(librariesClient,
      new JsonObject()
        .put("name", "Business Library")
        .put("campusId", jubileeCampus.toString()));

    ResourceClient locationsClient = ResourceClient.forLocations(client);

    final UUID fakeServicePointId = UUID.randomUUID();

    thirdFloorLocationId = createReferenceRecord(locationsClient,
      new JsonObject()
        .put("name", "3rd Floor")
        .put("code", "NU/JC/DL/3F")
        .put("institutionId", nottinghamUniversityInstitution.toString())
        .put("campusId", jubileeCampus.toString())
        .put("libraryId", djanoglyLibrary.toString())
        //TODO: Replace with created service point
        .put("primaryServicePoint", fakeServicePointId.toString())
        .put("servicePointIds", new JsonArray().add(fakeServicePointId.toString())));

    secondFloorEconomicsLocationId = createReferenceRecord(locationsClient,
      new JsonObject()
        .put("name", "2nd Floor - Economics")
        .put("code", "NU/JC/DL/2FE")
        .put("institutionId", nottinghamUniversityInstitution.toString())
        .put("campusId", jubileeCampus.toString())
        .put("libraryId", djanoglyLibrary.toString())
        //TODO: Replace with created service point
        .put("primaryServicePoint", fakeServicePointId.toString())
        .put("servicePointIds", new JsonArray().add(fakeServicePointId.toString())));

    mezzanineDisplayCaseLocationId = createReferenceRecord(locationsClient,
      new JsonObject()
        .put("name", "Display Case, Mezzanine")
        .put("code", "NU/JC/BL/DM")
        .put("institutionId", nottinghamUniversityInstitution.toString())
        .put("campusId", jubileeCampus.toString())
        .put("libraryId", businessLibrary.toString())
        //TODO: Replace with created service point
        .put("primaryServicePoint", fakeServicePointId.toString())
        .put("servicePointIds", new JsonArray().add(fakeServicePointId.toString())));
  }

  private static void deleteLocations()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    final OkapiHttpClient client = createClient();

    ResourceClient locationsClient = ResourceClient.forLocations(client);

    //Use the same ID as old locations for continuity
    locationsClient.delete(thirdFloorLocationId);
    locationsClient.delete(mezzanineDisplayCaseLocationId);
    locationsClient.delete(secondFloorEconomicsLocationId);

    ResourceClient librariesClient = ResourceClient.forLibraries(client);

    librariesClient.delete(djanoglyLibrary);
    librariesClient.delete(businessLibrary);

    ResourceClient campusesClient = ResourceClient.forCampuses(client);

    campusesClient.delete(jubileeCampus);

    ResourceClient institutionsClient = ResourceClient.forInstitutions(client);

    institutionsClient.delete(nottinghamUniversityInstitution);
  }

  private static void createInstanceTypes()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    booksInstanceTypeId = createReferenceRecord(
      ResourceClient.forInstanceTypes(createClient()), "Books", "BO", "tests");
  }

  private static void deleteInstanceTypes()
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

  private static void deleteLoanPolicies()
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

  private static UUID createReferenceRecord(
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

  private static UUID createReferenceRecord(
    ResourceClient client,
    String name)
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {
    return createReferenceRecord(client, name, null, null);
  }

  private static UUID createReferenceRecord(
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

  private static void deleteCancellationReasons()
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

  public static boolean isNotInitialised() {
    return !initialised;
  }
}
