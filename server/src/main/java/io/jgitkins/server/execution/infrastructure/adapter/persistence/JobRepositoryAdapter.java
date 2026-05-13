package io.jgitkins.server.execution.infrastructure.adapter.persistence;

import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.execution.infrastructure.mapper.JobDomainMapper;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.persistence.mapper.JobEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.JobHistoryEntityMbgMapper;
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
public class JobRepositoryAdapter implements JobRepository {

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

            return Optional.ofNullable(jobDomainMapper.toDomain(entity, loadHistories(jobId)));
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during job loading", e);
        }
    }

    @Override
    @Transactional
    public Optional<Long> appendHistoryIfCurrent(Job job, JobHistory previousHistory) {
        try {
            Long jobIdLong = Long.parseLong(job.getId().getValue());

            Optional<JobHistoryEntity> latestPersistedHistory = findLatestHistory(jobIdLong);
            if (latestPersistedHistory.isEmpty() || !isSameHistory(latestPersistedHistory.get(), previousHistory)) {
                return Optional.empty();
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

    private List<JobHistory> loadHistories(Long jobId) {
        JobHistoryEntityCondition condition = new JobHistoryEntityCondition();
        condition.createCriteria().andJobIdEqualTo(jobId);
        condition.setOrderByClause("CREATED_AT ASC, ID ASC");

        return jobHistoryEntityMbgMapper.selectByCondition(condition).stream()
                .map(jobDomainMapper::toHistoryDomain)
                .toList();
    }

    private Optional<JobHistoryEntity> findLatestHistory(Long jobId) {
        JobHistoryEntityCondition condition = new JobHistoryEntityCondition();
        condition.createCriteria().andJobIdEqualTo(jobId);
        condition.setOrderByClause("CREATED_AT DESC, ID DESC");

        return jobHistoryEntityMbgMapper.selectByCondition(condition).stream().findFirst();
    }

    private boolean isSameHistory(JobHistoryEntity latestPersisted, JobHistory expectedPreviousHistory) {
        return String.valueOf(latestPersisted.getId()).equals(expectedPreviousHistory.getId().getValue())
                && latestPersisted.getStatus().equals(expectedPreviousHistory.getStatus().name())
                && latestPersisted.getCreatedAt().equals(expectedPreviousHistory.getCreatedAt());
    }
}
