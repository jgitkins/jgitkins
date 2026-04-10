package io.jgitkins.server.application.support.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import io.jgitkins.server.application.dto.command.JobCreateCommand;
import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.result.JobPlan;
import io.jgitkins.server.application.port.in.JobCreateUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExecutionRequestServiceTest {

    @Mock
    private JobCreateUseCase jobCreateUseCase;

    @InjectMocks
    private ExecutionRequestService executionRequestService;

    @Test
    void requestPushExecution_mapsPlanAndCommandIntoJobCreateCommand() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(9L)
                .repoName("repo")
                .branchName("main")
                .commitHash("abc")
                .triggeredBy(7L)
                .build();

        executionRequestService.requestPushExecution(command, JobPlan.create(".jgitkins/pipelines/main.Jenkinsfile"));

        ArgumentCaptor<JobCreateCommand> captor = ArgumentCaptor.forClass(JobCreateCommand.class);
        verify(jobCreateUseCase).create(captor.capture());
        JobCreateCommand jobCreateCommand = captor.getValue();
        assertEquals("repo", jobCreateCommand.repoName());
        assertEquals(9L, jobCreateCommand.repositoryId());
        assertEquals("main", jobCreateCommand.branchName());
        assertEquals("abc", jobCreateCommand.commitHash());
        assertEquals(".jgitkins/pipelines/main.Jenkinsfile", jobCreateCommand.pipelineFilePath());
        assertEquals(7L, jobCreateCommand.triggeredBy());
    }
}
