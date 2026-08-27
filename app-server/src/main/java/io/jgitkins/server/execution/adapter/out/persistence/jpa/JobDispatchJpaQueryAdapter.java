package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.execution.application.contract.internal.DispatchableJob;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.domain.vo.ExecutionActorId;
import io.jgitkins.server.execution.domain.vo.ExecutionRepositoryId;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.CommitHash;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of {@link JobDispatchQueryPort}.
 *
 * <p>{@code @Transactional(readOnly = true)} is carried over from the MyBatis adapter and is not
 * decoration: the projection and the history load are two statements, and without one transaction a
 * job could be selected and then have its history read after another dispatcher advanced it. The
 * decision would be made from a mix of two points in time.
 */
@RequiredArgsConstructor
public class JobDispatchJpaQueryAdapter implements JobDispatchQueryPort {

    private static final String ORGANIZATION_OWNER_TYPE = "ORGANIZATION";

    private final JobDispatchJpaRepository jobDispatchJpaRepository;
    private final JobHistoryJpaRepository jobHistoryJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<DispatchableJob> fetchNextJob(RunnerDispatchContext context) {
        try {
            return findNext(context).map(this::toDispatchableJob);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during fetching pending jobs", e);
        }
    }

    private Optional<JobDispatchJpaProjection> findNext(RunnerDispatchContext context) {
        // The scope switch is exhaustive over the enum rather than defaulting to global. A new scope
        // added later must not silently fall through to "every repository", which is the widest
        // possible answer to a question about access.
        return switch (context.dispatchScope()) {
            case GLOBAL -> jobDispatchJpaRepository.findNextForGlobalScope();
            case ORGANIZE -> jobDispatchJpaRepository.findNextForOrganizeScope(context.scopeTargetId());
            case REPOSITORY -> jobDispatchJpaRepository.findNextForRepositoryScope(context.scopeTargetId());
        };
    }

    private DispatchableJob toDispatchableJob(JobDispatchJpaProjection row) {
        List<JobHistory> histories = ExecutionJpaHistoryMapping.toDomain(
                jobHistoryJpaRepository.findAllByJobIdOrderByCreatedAtAscIdAsc(row.getJobId()));

        // The organize id is populated only for organization-owned repositories. For a user-owned
        // repository the owner id is a user id, and passing it through would hand the runner an
        // organization id that identifies a different entity entirely.
        Long organizeId = ORGANIZATION_OWNER_TYPE.equals(row.getRepositoryOwnerType())
                ? row.getRepositoryOwnerId()
                : null;

        return new DispatchableJob(
                row.getJobId(),
                Job.reconstruct(
                        JobId.of(String.valueOf(row.getJobId())),
                        ExecutionRepositoryId.of(row.getRepositoryId()),
                        CommitHash.of(row.getCommitHash()),
                        BranchName.of(row.getBranchName()),
                        ExecutionActorId.of(row.getTriggeredBy()),
                        row.getJobCreatedAt(),
                        histories),
                organizeId,
                row.getRepositoryClonePath());
    }
}
