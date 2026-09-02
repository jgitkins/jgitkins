package io.jgitkins.server.execution.application.service.internal;

import io.jgitkins.server.execution.application.contract.JobCreateCommand;
import io.jgitkins.server.shared.application.command.PushEventCommand;
import io.jgitkins.server.execution.application.contract.internal.JobPlan;
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
