package io.jgitkins.server.execution.domain.repository;

import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import java.util.Optional;

public interface JobRepository {
    void save(Job job);

    Optional<Job> findById(Long jobId);

    Optional<Long> appendHistoryIfCurrent(Job job, JobHistory expectedPreviousHistory);
}
