package io.jgitkins.server.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.pipeline.PipelineConfig;
import io.jgitkins.server.application.dto.pipeline.PipelineRule;
import io.jgitkins.server.application.dto.result.JobPlan;
import io.jgitkins.server.application.dto.result.PipelineSkipReason;
import io.jgitkins.server.application.dto.support.PushJobPlanRequest;
import io.jgitkins.server.application.port.out.FileGitPort;
import io.jgitkins.server.application.port.out.PipelineConfigPort;
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
    private FileGitPort fileGitPort;

    @InjectMocks
    private PushJobCreationPolicy policy;

    @Test
    void plan_returnsCreate_whenRuleMatchesAndFileExists() {
        when(configPort.read("1", "repo", "abc"))
                .thenReturn(new PipelineConfig(List.of(new PipelineRule(List.of("main"), "pipelines/main.Jenkinsfile"))));
        when(fileGitPort.exists("1", "repo", "abc", ".jgitkins/pipelines/main.Jenkinsfile"))
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
        when(fileGitPort.exists("1", "repo", "abc", ".jgitkins/pipelines/main.Jenkinsfile"))
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
