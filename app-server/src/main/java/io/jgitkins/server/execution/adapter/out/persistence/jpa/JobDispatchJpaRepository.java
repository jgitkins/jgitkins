package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * The dispatch query, translated as native SQL rather than JPQL.
 *
 * <p>Three reasons it stays SQL. It joins {@code JOB} to {@code REPOSITORY}, which live in different
 * bounded contexts — expressing that in JPQL would require the execution context to name the repository
 * context's entity, inventing a mapped association the schema has no foreign key for. It uses a
 * correlated {@code NOT EXISTS} over the same table to express "the newest history row is PENDING",
 * which JPQL can state but not as readably. And the MyBatis original is itself hand-written SQL in XML,
 * so hand-written SQL here is the translation with the fewest moving parts to get wrong.
 *
 * <p>Three methods rather than one {@code <choose>}: the MyBatis query composed its scope filter
 * conditionally, and a single native query would need either dead predicates guarded by null checks or
 * string concatenation. The branch is explicit and each variant is independently readable, matching the
 * approach taken for the repository visibility query in task 2.72.
 *
 * <p>This extends {@link Repository} rather than {@code JpaRepository}: the dispatch query is read-only
 * and there is no {@code JobDispatchJpaEntity} to save. Inheriting {@code save} and {@code deleteAll}
 * here would expose write operations on a projection that has no table of its own.
 */
public interface JobDispatchJpaRepository extends Repository<JobJpaEntity, Long> {

    String SELECT_AND_FROM = """
            SELECT
              JOB.ID AS jobId,
              JOB.REPOSITORY_ID AS repositoryId,
              JOB.COMMIT_HASH AS commitHash,
              JOB.BRANCH_NAME AS branchName,
              JOB.TRIGGERED_BY AS triggeredBy,
              JOB.CREATED_AT AS jobCreatedAt,
              REPOSITORY.OWNER_TYPE AS repositoryOwnerType,
              REPOSITORY.OWNER_ID AS repositoryOwnerId,
              REPOSITORY.CLONE_PATH AS repositoryClonePath
            FROM JOB JOB
            JOIN REPOSITORY REPOSITORY ON REPOSITORY.ID = JOB.REPOSITORY_ID
            WHERE EXISTS (
              SELECT 1
              FROM JOB_HISTORY JOB_HISTORY
              WHERE JOB_HISTORY.JOB_ID = JOB.ID
                AND JOB_HISTORY.STATUS = 'PENDING'
                AND NOT EXISTS (
                  SELECT 1
                  FROM JOB_HISTORY NEWER
                  WHERE NEWER.JOB_ID = JOB_HISTORY.JOB_ID
                    AND (
                      NEWER.CREATED_AT > JOB_HISTORY.CREATED_AT
                      OR (NEWER.CREATED_AT = JOB_HISTORY.CREATED_AT AND NEWER.ID > JOB_HISTORY.ID)
                    )
                )
            )
            """;

    String ORDER_AND_LIMIT = " ORDER BY JOB.CREATED_AT ASC, JOB.ID ASC LIMIT 1";

    @Query(value = SELECT_AND_FROM + ORDER_AND_LIMIT, nativeQuery = true)
    Optional<JobDispatchJpaProjection> findNextForGlobalScope();

    @Query(value = SELECT_AND_FROM
            + " AND REPOSITORY.OWNER_TYPE = 'ORGANIZATION' AND REPOSITORY.OWNER_ID = :scopeTargetId"
            + ORDER_AND_LIMIT, nativeQuery = true)
    Optional<JobDispatchJpaProjection> findNextForOrganizeScope(@Param("scopeTargetId") Long scopeTargetId);

    @Query(value = SELECT_AND_FROM
            + " AND REPOSITORY.ID = :scopeTargetId"
            + ORDER_AND_LIMIT, nativeQuery = true)
    Optional<JobDispatchJpaProjection> findNextForRepositoryScope(@Param("scopeTargetId") Long scopeTargetId);
}
