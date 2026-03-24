package io.jgitkins.server.application.dto.result;

public enum PipelineSkipReason {
    SKIPPED_NO_RULE,
    SKIPPED_PIPELINE_NOT_FOUND,
    SKIPPED_POLICY_ERROR
}
