package io.jgitkins.server.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.jgitkins.server.application.dto.command.JobCreateCommand;
import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.result.JobCreationDecision;
import io.jgitkins.server.application.dto.result.JobPlan;
import io.jgitkins.server.application.dto.result.PipelineSkipReason;
import io.jgitkins.server.application.dto.support.PushJobPlanRequest;
import io.jgitkins.server.application.port.in.JobCreateUseCase;
import io.jgitkins.server.application.port.out.BranchPersistencePort;
import io.jgitkins.server.application.support.PushJobCreationPolicy;
import io.jgitkins.server.application.validate.JobCreationValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushEventHandleServiceTest {

    @Mock
    private JobCreateUseCase jobCreateUseCase;

    @Mock
    private BranchPersistencePort branchPort;

    @Mock
    private JobCreationValidator jobCreationValidator;

    @Mock
    private PushJobCreationPolicy pushJobCreationPolicy;

    @InjectMocks
    private PushEventHandleService service;

    @Test
    void handle_createsJobWhenPushIsValid() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(9L)
                .namespace("1")
                .repoName("repo")
                .branchName("main")
                .branchCreated(true)
                .commitHash("abc")
                .triggeredBy(1L)
                .build();

        when(jobCreationValidator.validate(command)).thenReturn(JobCreationDecision.create());
        when(pushJobCreationPolicy.plan(new PushJobPlanRequest("1", "repo", "main", "abc")))
                .thenReturn(JobPlan.create(".jgitkins/pipelines/main.Jenkinsfile"));

        service.handle(command);

        verify(branchPort).save(any());

        ArgumentCaptor<JobCreateCommand> captor = ArgumentCaptor.forClass(JobCreateCommand.class);
        verify(jobCreateUseCase).create(captor.capture());
        JobCreateCommand job = captor.getValue();
        assertEquals("repo", job.getRepoName());
        assertEquals(9L, job.getRepositoryId());
        assertEquals(".jgitkins/pipelines/main.Jenkinsfile", job.getPipelineFilePath());
    }

    @Test
    void handle_skipsJobWhenPlannerReturnsSkip() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(9L)
                .namespace("1")
                .repoName("repo")
                .branchName("main")
                .commitHash("abc")
                .triggeredBy(1L)
                .build();

        when(jobCreationValidator.validate(command)).thenReturn(JobCreationDecision.create());
        when(pushJobCreationPolicy.plan(new PushJobPlanRequest("1", "repo", "main", "abc")))
                .thenReturn(JobPlan.skip(PipelineSkipReason.SKIPPED_NO_RULE));

        service.handle(command);

        verify(jobCreateUseCase, never()).create(any());
    }

    @Test
    void handle_skipsJobWhenPolicyReturnsErrorSkip() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(9L)
                .namespace("1")
                .repoName("repo")
                .branchName("main")
                .commitHash("abc")
                .triggeredBy(1L)
                .build();

        when(jobCreationValidator.validate(command)).thenReturn(JobCreationDecision.create());
        when(pushJobCreationPolicy.plan(new PushJobPlanRequest("1", "repo", "main", "abc")))
                .thenReturn(JobPlan.skip(PipelineSkipReason.SKIPPED_POLICY_ERROR));

        service.handle(command);

        verify(jobCreateUseCase, never()).create(any());
    }

    @Test
    void handle_skipsJobWhenValidatorReturnsSkip() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(9L)
                .namespace("1")
                .repoName("repo")
                .branchName("main")
                .commitHash("")
                .triggeredBy(1L)
                .build();

        when(jobCreationValidator.validate(command)).thenReturn(JobCreationDecision.skip("missing commit hash"));

        service.handle(command);

        verify(jobCreateUseCase, never()).create(any());
    }
}
