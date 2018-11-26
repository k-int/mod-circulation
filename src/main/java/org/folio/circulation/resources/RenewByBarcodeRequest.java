package org.folio.circulation.resources;

import static org.folio.circulation.support.HttpResult.succeeded;
import static org.folio.circulation.support.JsonPropertyFetcher.getProperty;
import static org.folio.circulation.support.ValidationErrorFailure.failedResult;
import static org.folio.circulation.support.ValidationErrorFailure.failure;

import org.apache.commons.lang3.StringUtils;
import org.folio.circulation.domain.FindByBarcodeQuery;
import org.folio.circulation.support.HttpResult;

import io.vertx.core.json.JsonObject;

public class RenewByBarcodeRequest implements FindByBarcodeQuery {
  static final String USER_BARCODE = "userBarcode";
  private static final String ITEM_BARCODE = "itemBarcode";

  private final String itemBarcode;
  private final String userBarcode;

  private RenewByBarcodeRequest(String itemBarcode, String userBarcode) {
    this.itemBarcode = itemBarcode;
    this.userBarcode = userBarcode;
  }

  public static HttpResult<RenewByBarcodeRequest> from(JsonObject json) {
    final String itemBarcode = getProperty(json, ITEM_BARCODE);

    if(StringUtils.isBlank(itemBarcode)) {
      return failedResult("Renewal request must have an item barcode", ITEM_BARCODE, null);
    }

    final String userBarcode = getProperty(json, USER_BARCODE);

    if(StringUtils.isBlank(userBarcode)) {
      return failedResult("Renewal request must have a user barcode", USER_BARCODE, null);
    }

    return succeeded(new RenewByBarcodeRequest(itemBarcode, userBarcode));
  }

  @Override
  public String getItemBarcode() {
    return itemBarcode;
  }

  @Override
  public String getUserBarcode() {
    return userBarcode;
  }

}
