package io.jgitkins.server.application.support.execution;

import io.jgitkins.server.execution.application.contract.command.JobCreateCommand;
import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.result.JobPlan;
import io.jgitkins.server.execution.application.port.in.JobCreateUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecutionRequestService {

    private final JobCreateUseCase jobCreateUseCase;

    public void requestPushExecution(PushEventCommand command, JobPlan plan) {
        jobCreateUseCase.create(new JobCreateCommand(
                command.getRepoName(),
                command.getRepositoryId(),
                command.getCommitHash(),
                command.getBranchName(),
                plan.getPipelineFilePath(),
                command.getTriggeredBy()
        ));
    }
}
