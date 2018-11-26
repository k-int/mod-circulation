package org.folio.circulation.resources;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.circulation.domain.FindByIdQuery;
import org.folio.circulation.support.HttpResult;

import static org.folio.circulation.support.HttpResult.succeeded;
import static org.folio.circulation.support.JsonPropertyFetcher.getProperty;
import static org.folio.circulation.support.ValidationErrorFailure.failedResult;
import static org.folio.circulation.support.ValidationErrorFailure.failure;

public class RenewByIdRequest implements FindByIdQuery {
  static final String USER_ID = "userId";
  private static final String ITEM_ID = "itemId";

  private final String itemId;
  private final String userId;

  private RenewByIdRequest(String itemId, String userId) {
    this.itemId = itemId;
    this.userId = userId;
  }

  public static HttpResult<RenewByIdRequest> from(JsonObject json) {
    final String itemBarcode = getProperty(json, ITEM_ID);

    if(StringUtils.isBlank(itemBarcode)) {
      return failedResult("Renewal request must have an item ID", ITEM_ID, null);
    }

    final String userBarcode = getProperty(json, USER_ID);

    if(StringUtils.isBlank(userBarcode)) {
      return failedResult("Renewal request must have a user ID", USER_ID, null);
    }

    return succeeded(new RenewByIdRequest(itemBarcode, userBarcode));
  }

  @Override
  public String getItemId() {
    return itemId;
  }

  @Override
  public String getUserId() {
    return userId;
  }

}
