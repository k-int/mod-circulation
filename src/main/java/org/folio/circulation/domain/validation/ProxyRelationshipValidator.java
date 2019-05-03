package org.folio.circulation.domain.validation;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.circulation.support.Result.of;
import static org.folio.circulation.support.Result.succeeded;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.folio.circulation.domain.MultipleRecords;
import org.folio.circulation.domain.ProxyRelationship;
import org.folio.circulation.domain.UserRelatedRecord;
import org.folio.circulation.support.Clients;
import org.folio.circulation.support.CollectionResourceClient;
import org.folio.circulation.support.CqlQuery;
import org.folio.circulation.support.Result;
import org.folio.circulation.support.ValidationErrorFailure;

public class ProxyRelationshipValidator {
  private final CollectionResourceClient proxyRelationshipsClient;
  private Supplier<ValidationErrorFailure> invalidRelationshipErrorSupplier;

  public ProxyRelationshipValidator(
    Clients clients,
    Supplier<ValidationErrorFailure> invalidRelationshipErrorSupplier) {

    this.proxyRelationshipsClient = clients.userProxies();
    this.invalidRelationshipErrorSupplier = invalidRelationshipErrorSupplier;
  }

  public <T extends UserRelatedRecord> CompletableFuture<Result<T>> refuseWhenInvalid(
    T userRelatedRecord) {

    //No need to validate as not proxied activity
    if (userRelatedRecord.getProxyUserId() == null) {
      return completedFuture(succeeded(userRelatedRecord));
    }

    return succeeded(userRelatedRecord).failAfter(
      v -> doesNotHaveActiveProxyRelationship(userRelatedRecord),
        v -> invalidRelationshipErrorSupplier.get());
  }

  private CompletableFuture<Result<Boolean>> doesNotHaveActiveProxyRelationship(
    UserRelatedRecord record) {

    return proxyRelationshipQuery(record.getProxyUserId(), record.getUserId())
      .after(query -> proxyRelationshipsClient.getMany(query, 1000)
      .thenApply(result -> result.next(
        response -> MultipleRecords.from(response, ProxyRelationship::new, "proxiesFor"))
      .map(MultipleRecords::getRecords)
        .map(relationships -> relationships.stream()
          .noneMatch(ProxyRelationship::isActive))));
  }

  private Result<CqlQuery> proxyRelationshipQuery(
    String proxyUserId,
    String sponsorUserId) {

    String validateProxyQuery = String.format("proxyUserId==%s and userId==%s",
      proxyUserId, sponsorUserId);

    return of(() -> new CqlQuery(validateProxyQuery));
  }
}
