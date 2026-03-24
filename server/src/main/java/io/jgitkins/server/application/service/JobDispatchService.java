package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.DispatchableJob;
import io.jgitkins.server.application.dto.JobDispatchScope;
import io.jgitkins.server.application.dto.RunnerDispatchContext;
import io.jgitkins.server.application.dto.command.DispatchJobCommand;
import io.jgitkins.server.application.dto.result.JobDispatchResult;
import io.jgitkins.server.application.port.in.JobDispatchUseCase;
import io.jgitkins.server.application.port.out.JobPersistencePort;
import io.jgitkins.server.application.port.out.RunnerPersistencePort;
import io.jgitkins.server.application.support.CloneUrlBuilder;
import io.jgitkins.server.domain.aggregate.Job;
import io.jgitkins.server.domain.aggregate.Runner;
import io.jgitkins.server.domain.model.JobHistory;
import io.jgitkins.server.domain.model.vo.RunnerId;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobDispatchService implements JobDispatchUseCase {

    private final JobPersistencePort jobPort;
    private final RunnerPersistencePort runnerPort;
    private final CloneUrlBuilder cloneUrlBuilder;

    @Override
    @Transactional
    public Optional<JobDispatchResult> dispatch(DispatchJobCommand command) {
        Optional<RunnerDispatchContext> runnerContext = resolveRunnerContext(command.runnerToken());
        if (runnerContext.isEmpty()) {
            return Optional.empty();
        }

        Optional<DispatchableJob> dispatchedJob = jobPort.findNextDispatchableJob(runnerContext.get());
        if (dispatchedJob.isEmpty()) {
            return Optional.empty();
        }

        return assignRunner(runnerContext.get(), dispatchedJob.get());
    }

    private Optional<RunnerDispatchContext> resolveRunnerContext(String runnerToken) {
        if (runnerToken == null || runnerToken.isBlank()) {
            log.warn("Runner token is missing");
            return Optional.empty();
        }

        Optional<Runner> runner = runnerPort.findByToken(runnerToken);
        if (runner.isEmpty()) {
            log.warn("Runner not found for token={}", runnerToken);
            return Optional.empty();
        }

        return Optional.of(toDispatchContext(runner.get()));
    }

    private RunnerDispatchContext toDispatchContext(Runner runner) {
        return new RunnerDispatchContext(
                runner.getId(),
                JobDispatchScope.valueOf(runner.getScopeType().name()),
                runner.getScopeTargetId()
        );
    }

    private Optional<JobDispatchResult> assignRunner(RunnerDispatchContext runnerContext,
                                                     DispatchableJob dispatchableJob) {
        Job job = dispatchableJob.job();
        JobHistory previousHistory = job.getLatestHistory();
        RunnerId runnerId = RunnerId.of(String.valueOf(runnerContext.runnerId()));
        job.publish(runnerId);

        Optional<Long> historyId = jobPort.saveHistory(job, previousHistory);
        if (historyId.isEmpty()) {
            log.debug("Job {} was already processed by another dispatcher", job.getId().getValue());
            return Optional.empty();
        }

        return Optional.of(buildDispatchResult(runnerContext, dispatchableJob, job, historyId.get()));
    }

    private JobDispatchResult buildDispatchResult(RunnerDispatchContext runnerContext,
                                                  DispatchableJob dispatchableJob,
                                                  Job job,
                                                  Long jobHistoryId) {
        return new JobDispatchResult(
                parseJobId(job),
                jobHistoryId,
                runnerContext.runnerId(),
                job.getRepositoryId().getValue(),
                dispatchableJob.organizeId(),
                job.getCommitHash().getValue(),
                job.getBranchName().getValue(),
                job.getTriggeredBy().getValue(),
                LocalDateTime.now(),
                cloneUrlBuilder.build(dispatchableJob.repositoryClonePath())
        );
    }

    private Long parseJobId(Job job) {
        try {
            return Long.parseLong(job.getId().getValue());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
