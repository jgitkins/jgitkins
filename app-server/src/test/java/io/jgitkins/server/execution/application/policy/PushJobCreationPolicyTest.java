package io.jgitkins.server.execution.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.jgitkins.server.execution.application.contract.external.PipelineConfig;
import io.jgitkins.server.execution.application.contract.internal.PipelineRule;
import io.jgitkins.server.execution.application.contract.internal.JobPlan;
import io.jgitkins.server.execution.application.contract.internal.PipelineSkipReason;
import io.jgitkins.server.execution.application.contract.internal.PushJobPlanRequest;
import io.jgitkins.server.execution.application.port.out.PipelineConfigPort;
import io.jgitkins.server.execution.application.port.out.PipelineFileLookupPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushJobCreationPolicyTest {

    @Mock
    private PipelineConfigPort configPort;

    @Mock
    private PipelineFileLookupPort pipelineFileLookupPort;

    @InjectMocks
    private PushJobCreationPolicy policy;

    @Test
    void plan_returnsCreate_whenRuleMatchesAndFileExists() {
        when(configPort.read("1", "repo", "abc"))
                .thenReturn(new PipelineConfig(List.of(new PipelineRule(List.of("main"), "pipelines/main.Jenkinsfile"))));
        when(pipelineFileLookupPort.exists("1", "repo", "abc", ".jgitkins/pipelines/main.Jenkinsfile"))
                .thenReturn(true);

        JobPlan result = policy.plan(new PushJobPlanRequest("1", "repo", "main", "abc"));

        assertThat(result.isSkipped()).isFalse();
        assertThat(result.getPipelineFilePath()).isEqualTo(".jgitkins/pipelines/main.Jenkinsfile");
    }

    @Test
    void plan_returnsSkipNoRule_whenNoRuleMatches() {
        when(configPort.read("1", "repo", "abc"))
                .thenReturn(new PipelineConfig(List.of(new PipelineRule(List.of("develop"), "pipelines/dev.Jenkinsfile"))));

        JobPlan result = policy.plan(new PushJobPlanRequest("1", "repo", "main", "abc"));

        assertThat(result.isSkipped()).isTrue();
        assertThat(result.getSkipReason()).isEqualTo(PipelineSkipReason.SKIPPED_NO_RULE);
    }

    @Test
    void plan_returnsSkipPipelineNotFound_whenFileDoesNotExist() {
        when(configPort.read("1", "repo", "abc"))
                .thenReturn(new PipelineConfig(List.of(new PipelineRule(List.of("main"), "pipelines/main.Jenkinsfile"))));
        when(pipelineFileLookupPort.exists("1", "repo", "abc", ".jgitkins/pipelines/main.Jenkinsfile"))
                .thenReturn(false);

        JobPlan result = policy.plan(new PushJobPlanRequest("1", "repo", "main", "abc"));

        assertThat(result.isSkipped()).isTrue();
        assertThat(result.getSkipReason()).isEqualTo(PipelineSkipReason.SKIPPED_PIPELINE_NOT_FOUND);
    }

    @Test
    void plan_returnsSkipPolicyError_whenUnexpectedExceptionOccurs() {
        when(configPort.read("1", "repo", "abc"))
                .thenThrow(new IllegalStateException("pipeline config load failed"));

        JobPlan result = policy.plan(new PushJobPlanRequest("1", "repo", "main", "abc"));

        assertThat(result.isSkipped()).isTrue();
        assertThat(result.getSkipReason()).isEqualTo(PipelineSkipReason.SKIPPED_POLICY_ERROR);
    }
}
