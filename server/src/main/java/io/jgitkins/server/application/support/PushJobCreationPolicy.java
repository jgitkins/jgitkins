package io.jgitkins.server.application.support;

import static io.jgitkins.server.application.dto.result.PipelineSkipReason.SKIPPED_NO_RULE;
import static io.jgitkins.server.application.dto.result.PipelineSkipReason.SKIPPED_PIPELINE_NOT_FOUND;

import io.jgitkins.server.application.dto.pipeline.PipelineConfig;
import io.jgitkins.server.application.dto.pipeline.PipelineRule;
import io.jgitkins.server.application.dto.result.JobPlan;
import io.jgitkins.server.application.dto.result.PipelineSkipReason;
import io.jgitkins.server.application.dto.support.PushJobPlanRequest;
import io.jgitkins.server.application.port.out.FileGitPort;
import io.jgitkins.server.application.port.out.PipelineConfigPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PushJobCreationPolicy {

    private static final String PIPELINE_ROOT = ".jgitkins/";

    private final PipelineConfigPort configPort;
    private final FileGitPort fileGitPort;

    public JobPlan plan(PushJobPlanRequest request) {
        try {
            PipelineConfig config = configPort.read(request.namespace(), request.repoName(), request.commitHash());
            PipelineRule rule = resolveRule(config, request.branchName());
            if (rule == null) {
                return JobPlan.skip(SKIPPED_NO_RULE);
            }

            String pipelineFilePath = toPipelineFilePath(rule.getFile());
            if (!fileGitPort.exists(request.namespace(), request.repoName(), request.commitHash(), pipelineFilePath)) {
                return JobPlan.skip(SKIPPED_PIPELINE_NOT_FOUND);
            }

            return JobPlan.create(pipelineFilePath);
        } catch (RuntimeException ex) {
            log.warn("push event job planning skipped due to policy error. repo=[{}] branch=[{}] commit=[{}]",
                    request.repoName(), request.branchName(), request.commitHash(), ex);
            return JobPlan.skip(PipelineSkipReason.SKIPPED_POLICY_ERROR);
        }
    }

    private PipelineRule resolveRule(PipelineConfig config, String branchName) {
        if (config == null) {
            return null;
        }
        return config.findRule(branchName).orElse(null);
    }

    private String toPipelineFilePath(String file) {
        if (file.startsWith(PIPELINE_ROOT)) {
            return file;
        }
        return PIPELINE_ROOT + file;
    }
}
