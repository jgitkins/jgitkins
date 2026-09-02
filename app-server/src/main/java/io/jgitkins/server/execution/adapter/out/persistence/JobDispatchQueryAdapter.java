package io.jgitkins.server.execution.adapter.out.persistence;

import io.jgitkins.server.execution.application.contract.external.DispatchableJob;
import io.jgitkins.server.execution.application.contract.external.RunnerDispatchContext;
import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.adapter.out.persistence.support.JobDomainMapper;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.execution.adapter.out.persistence.translator.JobDispatchQueryMapper;
import io.jgitkins.server.execution.adapter.out.persistence.translator.JobHistoryEntityMbgMapper;
import io.jgitkins.server.execution.adapter.out.persistence.model.DispatchableJobRow;
import io.jgitkins.server.execution.adapter.out.persistence.model.JobHistoryEntityCondition;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registered by {@code ExecutionJobDispatchQuerySelectorConfiguration}, not by component scanning.
 *
 * <p>The {@code @Component} annotation was removed in task 2.76: with a JPA implementation of
 * {@code JobDispatchQueryPort} on the classpath, scanning would register two candidates and the
 * injection point would be ambiguous.
 */
@RequiredArgsConstructor
public class JobDispatchQueryAdapter implements JobDispatchQueryPort {

    private final JobDispatchQueryMapper jobDispatchQueryMapper;
    private final JobHistoryEntityMbgMapper jobHistoryEntityMbgMapper;
    private final JobDomainMapper jobDomainMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<DispatchableJob> fetchNextJob(RunnerDispatchContext context) {
        try {
            return Optional.ofNullable(fetchNextJobRow(context))
                    .flatMap(this::toDispatchableJob);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during fetching pending jobs", e);
        }
    }

    private DispatchableJobRow fetchNextJobRow(RunnerDispatchContext context) {
        return jobDispatchQueryMapper.selectNextDispatchableJob(
                context.dispatchScope().name(),
                context.scopeTargetId()
        );
    }

    private Optional<DispatchableJob> toDispatchableJob(DispatchableJobRow row) {
        List<JobHistory> histories = getHistories(row.jobId());
        Long organizeId = "ORGANIZATION".equals(row.repositoryOwnerType()) ? row.repositoryOwnerId() : null;

        return Optional.of(new DispatchableJob(
                row.jobId(),
                jobDomainMapper.toDomain(row, histories),
                organizeId,
                row.repositoryClonePath()
        ));
    }

    private List<JobHistory> getHistories(Long jobId) {
        JobHistoryEntityCondition condition = new JobHistoryEntityCondition();
        condition.createCriteria().andJobIdEqualTo(jobId);
        condition.setOrderByClause("CREATED_AT ASC, ID ASC");

        return jobDomainMapper.toHistoryDomain(jobHistoryEntityMbgMapper.selectByCondition(condition));
    }
}
