package io.jgitkins.server.execution.application.contract.result;

public enum PipelineSkipReason {
    SKIPPED_NO_RULE,
    SKIPPED_PIPELINE_NOT_FOUND,
    SKIPPED_POLICY_ERROR
}
