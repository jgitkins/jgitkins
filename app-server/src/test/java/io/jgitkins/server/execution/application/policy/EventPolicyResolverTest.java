package io.jgitkins.server.execution.application.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.jgitkins.server.shared.application.command.PushEventCommand;
import io.jgitkins.server.execution.application.contract.result.JobPlan;
import io.jgitkins.server.execution.application.contract.internal.PushJobPlanRequest;
import io.jgitkins.server.execution.application.policy.EventPolicyResolver;
import io.jgitkins.server.execution.application.policy.PushJobCreationPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventPolicyResolverTest {

    @Mock
    private PushJobCreationPolicy pushJobCreationPolicy;

    @InjectMocks
    private EventPolicyResolver eventPolicyResolver;

    @Test
    void resolvePushPlan_delegatesToExistingPushPolicy() {
        PushEventCommand command = PushEventCommand.builder()
                .namespace("team")
                .repoName("repo")
                .branchName("main")
                .commitHash("abc123")
                .build();

        when(pushJobCreationPolicy.plan(new PushJobPlanRequest("team", "repo", "main", "abc123")))
                .thenReturn(JobPlan.create(".jgitkins/Jenkinsfile"));

        JobPlan jobPlan = eventPolicyResolver.resolvePushPlan(command);

        assertEquals(".jgitkins/Jenkinsfile", jobPlan.getPipelineFilePath());
    }
}
