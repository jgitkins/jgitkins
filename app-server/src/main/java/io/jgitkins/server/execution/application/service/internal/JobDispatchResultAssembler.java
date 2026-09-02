package io.jgitkins.server.execution.application.service.internal;

import io.jgitkins.server.execution.application.contract.external.DispatchableJob;
import io.jgitkins.server.execution.application.contract.external.RunnerDispatchContext;
import io.jgitkins.server.execution.application.contract.JobDispatchResult;
import io.jgitkins.server.execution.domain.aggregate.Job;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class JobDispatchResultAssembler {

    public JobDispatchResult assemble(RunnerDispatchContext runnerContext,
                                      DispatchableJob dispatchableJob,
                                      Job job,
                                      Long jobHistoryId,
                                      String cloneUrl) {
        return new JobDispatchResult(
                dispatchableJob.jobId(),
                jobHistoryId,
                runnerContext.runnerId(),
                job.getRepositoryId().getValue(),
                dispatchableJob.organizeId(),
                job.getCommitHash().getValue(),
                job.getBranchName().getValue(),
                job.getTriggeredBy().getValue(),
                LocalDateTime.now(),
                cloneUrl
        );
    }
}
