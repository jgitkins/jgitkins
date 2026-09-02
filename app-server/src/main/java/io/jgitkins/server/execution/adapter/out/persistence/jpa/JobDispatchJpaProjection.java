package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import java.time.LocalDateTime;

/**
 * Read-only projection for the next dispatchable job.
 *
 * <p>An interface projection rather than a record, because the query behind it is native SQL and Spring
 * Data binds native results to interface getters by column alias. The aliases in
 * {@code JobDispatchJpaRepository} are lowerCamel for exactly that reason, and
 * {@code JobDispatchJpaMappingTest} asserts every getter here has a matching alias — a rename on one
 * side alone produces a null field, not an error.
 *
 * <p>Mirrored {@code DispatchableJobRow}, the MyBatis projection, field for field, and the two were
 * kept separate rather than shared so that neither provider's mapping constrained the other's. Only
 * this one is left.
 */
public interface JobDispatchJpaProjection {

    Long getJobId();

    Long getRepositoryId();

    String getCommitHash();

    String getBranchName();

    Long getTriggeredBy();

    LocalDateTime getJobCreatedAt();

    String getRepositoryOwnerType();

    Long getRepositoryOwnerId();

    String getRepositoryClonePath();
}
