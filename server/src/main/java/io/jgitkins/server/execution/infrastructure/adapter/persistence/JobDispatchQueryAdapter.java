package io.jgitkins.server.execution.infrastructure.adapter.persistence;

import io.jgitkins.server.execution.application.contract.internal.DispatchableJob;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.infrastructure.mapper.JobDomainMapper;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.persistence.mapper.JobDispatchQueryMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.JobHistoryEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.DispatchableJobRow;
import io.jgitkins.server.infrastructure.persistence.model.JobHistoryEntityCondition;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JobDispatchQueryAdapter implements JobDispatchQueryPort {

    private final JobDispatchQueryMapper jobDispatchQueryMapper;
    private final JobHistoryEntityMbgMapper jobHistoryEntityMbgMapper;
    private final JobDomainMapper jobDomainMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<DispatchableJob> findNextDispatchableJob(RunnerDispatchContext context) {
        try {
            return Optional.ofNullable(findNextDispatchableJobRow(context))
                    .flatMap(this::toDispatchableJob);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during fetching pending jobs", e);
        }
    }

    private DispatchableJobRow findNextDispatchableJobRow(RunnerDispatchContext context) {
        return jobDispatchQueryMapper.selectNextDispatchableJob(
                context.dispatchScope().name(),
                context.scopeTargetId()
        );
    }

    private Optional<DispatchableJob> toDispatchableJob(DispatchableJobRow row) {
        List<JobHistory> histories = loadHistories(row.jobId());
        Long organizeId = "ORGANIZATION".equals(row.repositoryOwnerType()) ? row.repositoryOwnerId() : null;

        return Optional.of(new DispatchableJob(
                jobDomainMapper.toDomain(row, histories),
                organizeId,
                row.repositoryClonePath()
        ));
    }

    private List<JobHistory> loadHistories(Long jobId) {
        JobHistoryEntityCondition condition = new JobHistoryEntityCondition();
        condition.createCriteria().andJobIdEqualTo(jobId);
        condition.setOrderByClause("CREATED_AT ASC, ID ASC");

        return jobHistoryEntityMbgMapper.selectByCondition(condition).stream()
                .map(jobDomainMapper::toHistoryDomain)
                .toList();
    }
}
