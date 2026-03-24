package io.jgitkins.server.application.port.out;

import io.jgitkins.server.application.dto.DispatchableJob;
import io.jgitkins.server.application.dto.RunnerDispatchContext;
import io.jgitkins.server.domain.aggregate.Job;
import io.jgitkins.server.domain.model.JobHistory;
import java.util.Optional;

public interface JobPersistencePort {
    void save(Job job);

    Optional<DispatchableJob> findNextDispatchableJob(RunnerDispatchContext context);
    Optional<Long> saveHistory(Job job, JobHistory previousHistory);
    Optional<Job> findById(Long jobId);
}
