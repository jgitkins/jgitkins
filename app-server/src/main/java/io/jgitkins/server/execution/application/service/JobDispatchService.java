package io.jgitkins.server.execution.application.service;

import io.jgitkins.server.execution.application.contract.internal.DispatchableJob;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import io.jgitkins.server.execution.application.contract.command.DispatchJobCommand;
import io.jgitkins.server.execution.application.contract.result.JobDispatchResult;
import io.jgitkins.server.execution.application.port.in.JobDispatchUseCase;
import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.repository.application.support.CloneUrlBuilder;
import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.execution.application.support.JobDispatchResultAssembler;
import io.jgitkins.server.execution.application.support.RunnerDispatchContextResolver;
import io.jgitkins.server.execution.domain.vo.RunnerId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobDispatchService implements JobDispatchUseCase {

    private final RunnerDispatchContextResolver runnerDispatchContextResolver;
    private final JobDispatchResultAssembler jobDispatchResultAssembler;
    private final CloneUrlBuilder cloneUrlBuilder;

    private final JobDispatchQueryPort jobDispatchQueryPort;
    private final JobRepository jobRepository;

    @Override
    @Transactional
    public Optional<JobDispatchResult> dispatch(DispatchJobCommand command) {
        return runnerDispatchContextResolver.resolve(command.runnerToken())
                .flatMap(runnerContext -> jobDispatchQueryPort.fetchNextJob(runnerContext)
                .flatMap(dispatchableJob -> assignRunner(runnerContext, dispatchableJob)));
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

        String cloneUrl = cloneUrlBuilder.build(dispatchableJob.repositoryClonePath());
        return Optional.of(jobDispatchResultAssembler.assemble(
                runnerContext,
                dispatchableJob,
                job,
                historyId.get(),
                cloneUrl
        ));
    }
}
