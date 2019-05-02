package org.folio.circulation.support;

import static org.folio.circulation.support.Result.failed;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class CqlHelper {
  private CqlHelper() { }

  /**
   *
   * Creates a CQL query for matching a property to one of multiple values
   * intended to return multiple records. Typically used when fetching related
   * records e.g. fetching all groups for users, or items for loans
   *
   * @param prefixQueryFragment fragment of CQL to include at the beginning
   *                            e.g. status.name=="Open" AND
   * @param indexName Name of the index (property) to match values to
   * @param valuesToSearchFor Values to search for, query should match any
   *                          against the index
   * @return null if there are no values to search for, otherwise a CQL
   * query that includes a fragment if provided and a clause for matching any
   * of the values
   */
  public static Result<String> unencodedMultipleRecordsCqlQuery(
    String prefixQueryFragment,
    String indexName,
    Collection<String> valuesToSearchFor) {

    final Collection<String> filteredValues = filterNullValues(valuesToSearchFor);

    if(filteredValues.isEmpty()) {
      return failed(new ServerErrorFailure(
        "Cannot fetch multiple records when no IDs are provided"));
    }

    return Result.of(() ->
      multipleRecordsUnencodedCqlQuery(prefixQueryFragment, indexName, filteredValues));
  }

  private static List<String> filterNullValues(Collection<String> values) {
    return values.stream()
      .filter(Objects::nonNull)
      .map(String::toString)
      .filter(StringUtils::isNotBlank)
      .distinct()
      .collect(Collectors.toList());
  }

  private static String multipleRecordsUnencodedCqlQuery(
    String prefixQueryFragment, String indexName,
    Collection<String> filteredValues) {

    String valueQuery = String.format("%s==(%s)",
      indexName, String.join(" or ", filteredValues));

    if(StringUtils.isBlank(prefixQueryFragment)) {
      return valueQuery;
    }
    else {
      return String.format("%s %s", prefixQueryFragment, valueQuery);
    }
  }
}
