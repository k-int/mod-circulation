package org.folio.circulation.domain;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.circulation.domain.representations.RequestProperties;

import java.util.Objects;

import static org.folio.circulation.domain.RequestStatus.*;
import static org.folio.circulation.domain.representations.RequestProperties.STATUS;
import static org.folio.circulation.support.JsonPropertyFetcher.getIntegerProperty;
import static org.folio.circulation.support.JsonPropertyWriter.write;

public class Request implements ItemRelatedRecord, UserRelatedRecord {
  private final JsonObject representation;
  private final Item item;
  private final User requester;
  private final User proxy;
  private boolean changedPosition = false;

  public Request(
    JsonObject representation,
    Item item,
    User requester,
    User proxy) {

    this.representation = representation;
    this.item = item;
    this.requester = requester;
    this.proxy = proxy;
  }

  public static Request from(JsonObject representation) {
    return new Request(representation, null, null, null);
  }

  public JsonObject asJson() {
    return representation.copy();
  }

  boolean isFulfillable() {
    return StringUtils.equals(getFulfilmentPreference(),
      RequestFulfilmentPreference.HOLD_SHELF);
  }

  boolean isOpen() {
    RequestStatus status = getStatus();

    return Objects.equals(status, OPEN_AWAITING_PICKUP)
      || Objects.equals(status, OPEN_NOT_YET_FILLED);
  }

  boolean isCancelled() {
    return Objects.equals(getStatus(), CLOSED_CANCELLED);
  }

  private boolean isFulfilled() {
    return Objects.equals(getStatus(), CLOSED_FILLED);
  }

  public boolean isClosed() {
    //Alternatively, check status contains "Closed"
    return isCancelled() || isFulfilled();
  }

  boolean isAwaitingPickup() {
    return Objects.equals(getStatus(), OPEN_AWAITING_PICKUP);
  }

  boolean isFor(User user) {
    return StringUtils.equals(getUserId(), user.getId());
  }

  @Override
  public String getItemId() {
    return representation.getString("itemId");
  }

  public Request withItem(Item newItem) {
    return new Request(representation, newItem, requester, proxy);
  }

  public Request withRequester(User newRequester) {
    return new Request(representation, item, newRequester, proxy);
  }

  public Request withProxy(User newProxy) {
    return new Request(representation, item, requester, newProxy);
  }
  
  @Override
  public String getUserId() {
    return representation.getString("requesterId");
  }

  @Override
  public String getProxyUserId() {
    return representation.getString("proxyUserId");
  }

  String getFulfilmentPreference() {
    return representation.getString("fulfilmentPreference");
  }

  public String getId() {
    return representation.getString("id");
  }

  String getRequestType() {
    return representation.getString("requestType");
  }

  RequestStatus getStatus() {
    return RequestStatus.from(representation.getString(STATUS));
  }

  void changeStatus(RequestStatus status) {
    //TODO: Check for null status
    status.writeTo(representation);
  }

  public Item getItem() {
    return item;
  }

  public User getRequester() {
    return requester;
  }

  public User getProxy() {
    return proxy;
  }

  Request changePosition(Integer newPosition) {
    if(!Objects.equals(getPosition(), newPosition)) {
      write(representation, RequestProperties.POSITION, newPosition);
      changedPosition = true;
    }

    return this;
  }

  void removePosition() {
    representation.remove(RequestProperties.POSITION);
    changedPosition = true;
  }

  public Integer getPosition() {
    return getIntegerProperty(representation, RequestProperties.POSITION, null);
  }

  boolean hasChangedPosition() {
    return changedPosition;
  }
}
