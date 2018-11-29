package org.folio.circulation.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

//TODO: Rename to JsonObjectArrayHelper or consolidate with string version
public class JsonArrayHelper {
  private JsonArrayHelper() { }

  public static List<JsonObject> toList(JsonArray array) {
    if(array == null) {
      return new ArrayList<>();
    }

    return toStream(array)
      .collect(Collectors.toList());
  }

  public static <T> List<T> mapToList(
    JsonArray array,
    Function<JsonObject, T> mapper) {

    if(array == null) {
      return Collections.emptyList();
    }

    return toStream(array)
      .map(mapper)
      .collect(Collectors.toList());
  }

  public static <T> List<T> mapToList(
    JsonObject within,
    String arrayPropertyName,
    Function<JsonObject, T> mapper) {

    if(within == null || !within.containsKey(arrayPropertyName)) {
      return Collections.emptyList();
    }

    return mapToList(within.getJsonArray(arrayPropertyName), mapper);
  }

  public static Stream<JsonObject> toStream(
    JsonObject within,
    String arrayPropertyName) {

    if(within == null || !within.containsKey(arrayPropertyName)) {
      return Stream.empty();
    }

    return toStream(within.getJsonArray(arrayPropertyName));
  }

  private static Stream<JsonObject> toStream(JsonArray array) {
    return array
      .stream()
      .map(castToJsonObject())
      .filter(Objects::nonNull);
  }

  private static Function<Object, JsonObject> castToJsonObject() {
    return loan -> {
      if(loan instanceof JsonObject) {
        return (JsonObject)loan;
      }
      else {
        return null;
      }
    };
  }
}
