package io.jgitkins.server.execution.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jgitkins.server.shared.application.command.PushEventCommand;
import io.jgitkins.server.execution.application.contract.result.JobCreationDecision;
import io.jgitkins.server.execution.application.contract.result.JobPlan;
import io.jgitkins.server.execution.application.contract.result.PipelineSkipReason;
import io.jgitkins.server.shared.application.support.change.BranchChangeRecorder;
import io.jgitkins.server.execution.application.support.ExecutionRequestService;
import io.jgitkins.server.execution.application.policy.EventPolicyResolver;
import io.jgitkins.server.execution.application.validate.JobCreationValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushEventHandleServiceTest {

    @Mock
    private BranchChangeRecorder branchChangeRecorder;

    @Mock
    private JobCreationValidator jobCreationValidator;

    @Mock
    private EventPolicyResolver eventPolicyResolver;

    @Mock
    private ExecutionRequestService executionRequestService;

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
        when(eventPolicyResolver.resolvePushPlan(command))
                .thenReturn(JobPlan.create(".jgitkins/pipelines/main.Jenkinsfile"));

        service.handle(command);

        verify(branchChangeRecorder).record(command);
        ArgumentCaptor<JobPlan> jobPlanCaptor = ArgumentCaptor.forClass(JobPlan.class);
        verify(executionRequestService).requestPushExecution(org.mockito.ArgumentMatchers.eq(command), jobPlanCaptor.capture());
        assertEquals(".jgitkins/pipelines/main.Jenkinsfile", jobPlanCaptor.getValue().getPipelineFilePath());
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
        when(eventPolicyResolver.resolvePushPlan(command))
                .thenReturn(JobPlan.skip(PipelineSkipReason.SKIPPED_NO_RULE));

        service.handle(command);

        verify(branchChangeRecorder).record(command);
        verify(executionRequestService, never()).requestPushExecution(any(), any());
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
        when(eventPolicyResolver.resolvePushPlan(command))
                .thenReturn(JobPlan.skip(PipelineSkipReason.SKIPPED_POLICY_ERROR));

        service.handle(command);

        verify(branchChangeRecorder).record(command);
        verify(executionRequestService, never()).requestPushExecution(any(), any());
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

        verify(branchChangeRecorder).record(command);
        verifyNoInteractions(eventPolicyResolver);
        verifyNoInteractions(executionRequestService);
    }
}
