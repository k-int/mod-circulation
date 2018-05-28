package api.support.builders;

import io.vertx.core.json.JsonObject;
import org.folio.circulation.support.http.client.IndividualResource;
import org.joda.time.DateTime;

public class CheckOutByBarcodeRequestBuilder extends JsonBuilder implements Builder {
  private final String itemBarcode;
  private final String userBarcode;
  private final String proxyBarcode;
  private final DateTime loanDate;

  public CheckOutByBarcodeRequestBuilder() {
    this(null, null, null, null);
  }

  private CheckOutByBarcodeRequestBuilder(
    String itemBarcode,
    String userBarcode, String proxyBarcode, DateTime loanDate) {

    this.itemBarcode = itemBarcode;
    this.userBarcode = userBarcode;
    this.proxyBarcode = proxyBarcode;
    this.loanDate = loanDate;
  }

  @Override
  public JsonObject create() {
    final JsonObject request = new JsonObject();

    put(request, "itemBarcode", this.itemBarcode);
    put(request, "userBarcode", this.userBarcode);
    put(request, "proxyUserBarcode", this.proxyBarcode);
    put(request, "loanDate", this.loanDate);

    return request;
  }

  public CheckOutByBarcodeRequestBuilder forItem(IndividualResource item) {
    return forItemBarcode(getBarcode(item));
  }

  public CheckOutByBarcodeRequestBuilder forItemBarcode(String itemBarcode) {
    return new CheckOutByBarcodeRequestBuilder(
      itemBarcode,
      this.userBarcode,
      this.proxyBarcode,
      this.loanDate);
  }

  public CheckOutByBarcodeRequestBuilder to(IndividualResource loanee) {
    return toUserBarcode(getBarcode(loanee));
  }

  public CheckOutByBarcodeRequestBuilder toUserBarcode(String userBarcode) {
    return new CheckOutByBarcodeRequestBuilder(
      this.itemBarcode,
      userBarcode,
      this.proxyBarcode,
      this.loanDate);
  }

  public CheckOutByBarcodeRequestBuilder at(DateTime loanDate) {
    return new CheckOutByBarcodeRequestBuilder(
      this.itemBarcode,
      this.userBarcode,
      this.proxyBarcode,
      loanDate);
  }

  public CheckOutByBarcodeRequestBuilder proxiedBy(IndividualResource proxy) {
    return new CheckOutByBarcodeRequestBuilder(
      this.itemBarcode,
      this.userBarcode,
      getBarcode(proxy),
      this.loanDate);
  }

  private String getBarcode(IndividualResource record) {
    return record.getJson().getString("barcode");
  }
}
