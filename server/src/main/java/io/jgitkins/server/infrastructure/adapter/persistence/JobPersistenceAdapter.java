package io.jgitkins.server.infrastructure.adapter.persistence;

import io.jgitkins.server.application.dto.DispatchableJob;
import io.jgitkins.server.application.dto.RunnerDispatchContext;
import io.jgitkins.server.application.port.out.JobPersistencePort;
import io.jgitkins.server.domain.aggregate.Job;
import io.jgitkins.server.domain.model.JobHistory;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.mapper.JobDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.JobDispatchQueryMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.JobEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.JobHistoryEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.DispatchableJobRow;
import io.jgitkins.server.infrastructure.persistence.model.JobEntity;
import io.jgitkins.server.infrastructure.persistence.model.JobHistoryEntity;
import io.jgitkins.server.infrastructure.persistence.model.JobHistoryEntityCondition;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JobPersistenceAdapter implements JobPersistencePort {

    private final JobDispatchQueryMapper jobDispatchQueryMapper;
    private final JobEntityMbgMapper jobEntityMbgMapper;
    private final JobHistoryEntityMbgMapper jobHistoryEntityMbgMapper;
    private final JobDomainMapper jobDomainMapper;

    @Override
    @Transactional
    public void save(Job job) {
        try {
            JobEntity entity = jobDomainMapper.toEntity(job);
            jobEntityMbgMapper.insertSelective(entity);

            Long generatedId = entity.getId();
            for (JobHistory history : job.getHistories()) {
                JobHistoryEntity historyEntity = jobDomainMapper.toHistoryEntity(history, generatedId);
                jobHistoryEntityMbgMapper.insertSelective(historyEntity);
            }
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during job creation", e);
        }
    }

    @Override
    public Optional<Job> findById(Long jobId) {
        try {
            JobEntity entity = jobEntityMbgMapper.selectByPrimaryKey(jobId);
            if (entity == null) {
                return Optional.empty();
            }

            JobHistoryEntityCondition historyCondition = new JobHistoryEntityCondition();
            historyCondition.createCriteria().andJobIdEqualTo(jobId);
            historyCondition.setOrderByClause("CREATED_AT ASC");

            List<JobHistory> histories = jobHistoryEntityMbgMapper.selectByCondition(historyCondition).stream()
                    .map(jobDomainMapper::toHistoryDomain)
                    .toList();

            return Optional.ofNullable(jobDomainMapper.toDomain(entity, histories));
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during job loading", e);
        }
    }

    @Override
    @Transactional
    public Optional<DispatchableJob> findNextDispatchableJob(RunnerDispatchContext context) {
        try {
            return Optional.ofNullable(findNextDispatchableJobRow(context))
                           .flatMap(this::toDispatchableJob);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during fetching pending jobs", e);
        }
    }

    @Override
    @Transactional
    public Optional<Long> saveHistory(Job job, JobHistory previousHistory) {
        try {
            // Optimistic locking: check if the previous history is still the latest
            Long jobIdLong = Long.parseLong(job.getId().getValue());

            JobHistoryEntityCondition condition = new JobHistoryEntityCondition();
            condition.createCriteria()
                    .andJobIdEqualTo(jobIdLong)
                    .andStatusEqualTo(previousHistory.getStatus().name())
                    .andCreatedAtEqualTo(previousHistory.getCreatedAt());

            // If we were using seqNo, it would be better.

            long count = jobHistoryEntityMbgMapper.countByCondition(condition);
            if (count == 0) {
                return Optional.empty(); // Stale data
            }

            JobHistory latest = job.getLatestHistory();
            JobHistoryEntity entity = jobDomainMapper.toHistoryEntity(latest, jobIdLong);
            jobHistoryEntityMbgMapper.insertSelective(entity);

            return Optional.of(entity.getId());
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during history persistence", e);
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
        JobHistoryEntityCondition historyCondition = new JobHistoryEntityCondition();
        historyCondition.createCriteria().andJobIdEqualTo(jobId);
        historyCondition.setOrderByClause("CREATED_AT ASC, ID ASC");

        return jobHistoryEntityMbgMapper.selectByCondition(historyCondition).stream()
                                        .map(jobDomainMapper::toHistoryDomain)
                                        .toList();
    }
}
