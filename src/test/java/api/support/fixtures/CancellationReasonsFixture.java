package api.support.fixtures;

import static org.folio.circulation.support.JsonPropertyFetcher.getProperty;
import static org.folio.circulation.support.JsonPropertyWriter.write;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.circulation.support.http.client.IndividualResource;

import api.support.http.ResourceClient;
import io.vertx.core.json.JsonObject;

public class CancellationReasonsFixture {
  private final RecordCreator cancellationReasonsRecordCreator;

  public CancellationReasonsFixture(ResourceClient cancellationReasonsClient) {
    cancellationReasonsRecordCreator = new RecordCreator(cancellationReasonsClient,
      reason -> getProperty(reason, "name"));
  }

  public void cleanUp()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    cancellationReasonsRecordCreator.cleanUp();
  }

  public IndividualResource courseReserves()
      throws InterruptedException, 
      MalformedURLException, 
      TimeoutException, 
      ExecutionException {

    return createReason("Course Reserves", "Item needed for course reserves");
  }

  private IndividualResource createReason(String name, String description)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    final JsonObject reason = new JsonObject();

    write(reason, "name", name);
    write(reason, "description", description);

    return cancellationReasonsRecordCreator.createIfAbsent(reason);
  }
}