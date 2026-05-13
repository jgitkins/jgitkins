package io.jgitkins.server.execution.application.service;

import io.jgitkins.server.execution.application.contract.internal.DispatchableJob;
import io.jgitkins.server.execution.application.contract.internal.JobDispatchScope;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import io.jgitkins.server.execution.application.contract.command.DispatchJobCommand;
import io.jgitkins.server.execution.application.contract.result.JobDispatchResult;
import io.jgitkins.server.execution.application.port.in.JobDispatchUseCase;
import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.application.support.CloneUrlBuilder;
import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.execution.domain.vo.RunnerId;
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

    private final JobDispatchQueryPort jobDispatchQueryPort;
    private final JobRepository jobRepository;
    private final RunnerRepository runnerRepository;
    private final CloneUrlBuilder cloneUrlBuilder;

    @Override
    @Transactional
    public Optional<JobDispatchResult> dispatch(DispatchJobCommand command) {
        Optional<RunnerDispatchContext> runnerContext = resolveRunnerContext(command.runnerToken());
        if (runnerContext.isEmpty()) {
            return Optional.empty();
        }

        Optional<DispatchableJob> dispatchedJob = jobDispatchQueryPort.findNextDispatchableJob(runnerContext.get());
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

        Optional<Runner> runner = runnerRepository.findByToken(runnerToken);
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

        Optional<Long> historyId = jobRepository.appendHistoryIfCurrent(job, previousHistory);
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
