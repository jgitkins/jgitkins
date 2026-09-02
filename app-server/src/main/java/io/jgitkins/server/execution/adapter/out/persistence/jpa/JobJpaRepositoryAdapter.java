package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.execution.domain.vo.ExecutionActorId;
import io.jgitkins.server.execution.domain.vo.ExecutionRepositoryId;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.execution.domain.vo.RunnerId;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.CommitHash;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of {@link JobRepository}.
 *
 * <p>The interesting method is {@link #appendHistoryIfCurrent}, which is a compare-and-set, not a
 * plain insert. It locks the current latest history row, re-reads it, refuses if it is no longer the
 * one the caller based its decision on, and only then appends. Two dispatchers racing for the same job
 * serialize on that lock; the loser sees a different latest row and returns empty rather than
 * appending a second transition. Dropping the lock would leave both of them appending, and the job
 * would run twice.
 *
 * <p>{@code @Transactional} is kept on the same two methods the MyBatis adapter annotated. The lock is
 * held until commit, so the boundary is the guarantee, not a decoration: a lock taken and released
 * before the insert would protect nothing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobJpaRepositoryAdapter implements JobRepository {

    private static final PageRequest LATEST_ONE = PageRequest.of(0, 1);

    private final JobJpaRepository jobJpaRepository;
    private final JobHistoryJpaRepository jobHistoryJpaRepository;

    @Override
    @Transactional
    public void save(Job job) {
        try {
            JobJpaEntity saved = jobJpaRepository.save(toEntity(job));
            for (JobHistory history : job.getHistories()) {
                jobHistoryJpaRepository.save(toHistoryEntity(history, saved.getId()));
            }
        } catch (Exception e) {
            throw persistence("Database operation failed during job creation", e);
        }
    }

    @Override
    public Optional<Job> findById(Long jobId) {
        try {
            return jobJpaRepository.findById(jobId)
                    .map(entity -> toDomain(entity, histories(jobId)));
        } catch (Exception e) {
            throw persistence("Database operation failed during job loading", e);
        }
    }

    @Override
    @Transactional
    public Optional<Long> appendHistoryIfCurrent(Job job, JobHistory expectedPreviousHistory) {
        try {
            Long jobId = Long.parseLong(job.getId().getValue());

            // Take the row lock first, then read what is actually there. The lock is what makes the
            // comparison below meaningful; without it the winner could be decided after the check.
            jobHistoryJpaRepository.lockLatestForJob(jobId, LATEST_ONE);
            JobHistoryJpaEntity latestPersisted = jobHistoryJpaRepository
                    .findLatestForJob(jobId, LATEST_ONE).stream().findFirst().orElse(null);

            if (latestPersisted == null || !isSameHistory(latestPersisted, expectedPreviousHistory)) {
                return Optional.empty();
            }

            JobHistoryJpaEntity appended = jobHistoryJpaRepository
                    .save(toHistoryEntity(job.getLatestHistory(), jobId));
            return Optional.of(appended.getId());
        } catch (Exception e) {
            throw persistence("Database operation failed during history persistence", e);
        }
    }

    private List<JobHistory> histories(Long jobId) {
        // The sequence number is positional, not stored. The rule lives in ExecutionJpaHistoryMapping
        // so the dispatch query adapter cannot derive it differently -- both paths compare history
        // identity to decide who wins a dispatch.
        return ExecutionJpaHistoryMapping.toDomain(
                jobHistoryJpaRepository.findAllByJobIdOrderByCreatedAtAscIdAsc(jobId));
    }

    private boolean isSameHistory(JobHistoryJpaEntity latestPersisted, JobHistory expectedPrevious) {
        return String.valueOf(latestPersisted.getId()).equals(expectedPrevious.getId().getValue())
                && latestPersisted.getStatus().equals(expectedPrevious.getStatus().name())
                && latestPersisted.getCreatedAt().equals(expectedPrevious.getCreatedAt());
    }

    private JobJpaEntity toEntity(Job job) {
        JobJpaEntity entity = new JobJpaEntity();
        // The id is deliberately left null: JOB.ID is auto-increment and the MyBatis mapper ignored
        // the domain id on insert too, so a job save is always an insert.
        entity.setRepositoryId(job.getRepositoryId().getValue());
        entity.setCommitHash(job.getCommitHash().getValue());
        entity.setBranchName(job.getBranchName().getValue());
        entity.setTriggeredBy(job.getTriggeredBy().getValue());
        entity.setCreatedAt(job.getCreatedAt());
        return entity;
    }

    private JobHistoryJpaEntity toHistoryEntity(JobHistory history, Long jobId) {
        JobHistoryJpaEntity entity = new JobHistoryJpaEntity();
        entity.setJobId(jobId);
        entity.setStatus(history.getStatus().name());
        entity.setCreatedAt(history.getCreatedAt());
        entity.setRunnerId(numericRunnerId(history.getRunnerId()));
        return entity;
    }

    private Long numericRunnerId(RunnerId runnerId) {
        if (runnerId == null) {
            return null;
        }
        String value = runnerId.getValue();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            // Matches the MyBatis mapper: a non-numeric runner id is stored as null rather than
            // failing the write, and the discrepancy is logged instead of swallowed.
            log.warn("RunnerId [{}] is not numeric. runner_id will be stored as null.", value);
            return null;
        }
    }

    private Job toDomain(JobJpaEntity entity, List<JobHistory> histories) {
        return Job.reconstruct(
                JobId.of(String.valueOf(entity.getId())),
                ExecutionRepositoryId.of(entity.getRepositoryId()),
                CommitHash.of(entity.getCommitHash()),
                BranchName.of(entity.getBranchName()),
                ExecutionActorId.of(entity.getTriggeredBy()),
                entity.getCreatedAt(),
                histories);
    }

    private InfrastructureException persistence(String message, Exception cause) {
        return new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, message, cause);
    }
}
