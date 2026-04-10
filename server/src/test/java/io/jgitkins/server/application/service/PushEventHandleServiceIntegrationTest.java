package io.jgitkins.server.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.result.JobCreationDecision;
import io.jgitkins.server.application.dto.result.JobPlan;
import io.jgitkins.server.application.port.in.JobCreateUseCase;
import io.jgitkins.server.application.support.PushJobCreationPolicy;
import io.jgitkins.server.application.support.change.BranchChangeRecorder;
import io.jgitkins.server.application.support.execution.ExecutionRequestService;
import io.jgitkins.server.application.support.policy.EventPolicyResolver;
import io.jgitkins.server.application.validate.JobCreationValidator;
import io.jgitkins.server.domain.repository.BranchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {
        PushEventHandleService.class,
        BranchChangeRecorder.class,
        EventPolicyResolver.class,
        ExecutionRequestService.class
})
class PushEventHandleServiceIntegrationTest {

    @Autowired
    private PushEventHandleService service;

    @MockBean
    private BranchRepository branchRepository;

    @MockBean
    private JobCreationValidator jobCreationValidator;

    @MockBean
    private PushJobCreationPolicy pushJobCreationPolicy;

    @MockBean
    private JobCreateUseCase jobCreateUseCase;

    @Test
    void handle_wiresCollaboratorsAndRequestsExecutionWhenPlanExists() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(99L)
                .namespace("team")
                .repoName("repo")
                .branchName("main")
                .branchCreated(true)
                .commitHash("abc123")
                .triggeredBy(7L)
                .build();

        when(jobCreationValidator.validate(command)).thenReturn(JobCreationDecision.create());
        when(pushJobCreationPolicy.plan(any())).thenReturn(JobPlan.create(".jgitkins/Jenkinsfile"));

        service.handle(command);

        verify(branchRepository).save(any());
        verify(jobCreateUseCase).create(any());
    }

    @Test
    void handle_skipsExecutionWhenValidatorRejectsEvent() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(99L)
                .namespace("team")
                .repoName("repo")
                .branchName("main")
                .commitHash("")
                .triggeredBy(7L)
                .build();

        when(jobCreationValidator.validate(command)).thenReturn(JobCreationDecision.skip("missing commit hash"));

        service.handle(command);

        verify(pushJobCreationPolicy, never()).plan(any());
        verify(jobCreateUseCase, never()).create(any());
    }
}
