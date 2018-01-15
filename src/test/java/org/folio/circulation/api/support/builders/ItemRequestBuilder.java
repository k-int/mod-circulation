package org.folio.circulation.api.support.builders;

import io.vertx.core.json.JsonObject;
import org.folio.circulation.api.APITestSuite;

import java.util.UUID;

public class ItemRequestBuilder implements Builder {

  private static final String AVAILABLE_STATUS = "Available";
  private static final String CHECKED_OUT_STATUS = "Checked out";

  private final UUID id;
  private final UUID holdingId;
  private final String barcode;
  private final String status;
  private final UUID materialTypeId;
  private final UUID temporaryLocationId;
  private final UUID temporaryLoanTypeId;

  public ItemRequestBuilder() {
    this(UUID.randomUUID(), null, "565578437802", AVAILABLE_STATUS,
      null, null, null);
  }

  private ItemRequestBuilder(
    UUID id,
    UUID holdingId,
    String barcode,
    String status,
    UUID temporaryLocationId,
    UUID materialTypeId,
    UUID temporaryLoanTypeId) {

    this.id = id;
    this.holdingId = holdingId;
    this.barcode = barcode;
    this.status = status;
    this.temporaryLocationId = temporaryLocationId;
    this.materialTypeId = materialTypeId;
    this.temporaryLoanTypeId = temporaryLoanTypeId;
  }

  public JsonObject create() {
    JsonObject itemRequest = new JsonObject();

    if(id != null) {
      itemRequest.put("id", id.toString());
    }

    if(barcode != null) {
      itemRequest.put("barcode", barcode);
    }

    if(holdingId != null) {
      itemRequest.put("holdingsRecordId", holdingId.toString());
    }

    if(materialTypeId != null) {
      itemRequest.put("materialTypeId", materialTypeId.toString());
    }

    itemRequest.put("status", new JsonObject().put("name", status));
    itemRequest.put("permanentLoanTypeId", APITestSuite.canCirculateLoanTypeId().toString());

    if(temporaryLocationId != null) {
      itemRequest.put("temporaryLocationId", temporaryLocationId.toString());
    }

    if(temporaryLoanTypeId != null) {
      itemRequest.put("temporaryLoanTypeId", temporaryLoanTypeId.toString());
    }

    return itemRequest;
  }

  public ItemRequestBuilder checkOut() {
    return withStatus(CHECKED_OUT_STATUS);
  }

  public ItemRequestBuilder available() {
    return withStatus(AVAILABLE_STATUS);
  }

  public ItemRequestBuilder withStatus(String status) {
    return new ItemRequestBuilder(
      this.id,
      this.holdingId,
      this.barcode,
      status,
      this.temporaryLocationId,
      this.materialTypeId,
      this.temporaryLoanTypeId);
  }

  public ItemRequestBuilder withBarcode(String barcode) {
    return new ItemRequestBuilder(
      this.id,
      this.holdingId,
      barcode,
      this.status,
      this.temporaryLocationId,
      this.materialTypeId,
      this.temporaryLoanTypeId);
  }

  public ItemRequestBuilder withNoBarcode() {
    return withBarcode(null);
  }

  public ItemRequestBuilder withNoTemporaryLocation() {
    return withTemporaryLocation(null);
  }

  public ItemRequestBuilder withTemporaryLocation(UUID temporaryLocationId) {
    return new ItemRequestBuilder(
      this.id,
      this.holdingId,
      this.barcode,
      this.status,
      temporaryLocationId,
      this.materialTypeId,
      this.temporaryLoanTypeId);
  }

  public ItemRequestBuilder forHolding(UUID holdingId) {
    return new ItemRequestBuilder(
      this.id,
      holdingId,
      this.barcode,
      this.status,
      this.temporaryLocationId,
      this.materialTypeId,
      this.temporaryLoanTypeId);
  }

  public ItemRequestBuilder withMaterialType(UUID materialTypeId) {
    return new ItemRequestBuilder(
      this.id,
      this.holdingId,
      this.barcode,
      this.status,
      this.temporaryLocationId,
      materialTypeId,
      this.temporaryLoanTypeId);
  }

  public ItemRequestBuilder withTemporaryLoanType(UUID loanTypeId) {
    return new ItemRequestBuilder(
      this.id,
      this.holdingId,
      this.barcode,
      this.status,
      this.temporaryLocationId,
      this.materialTypeId,
      loanTypeId);

  }
}