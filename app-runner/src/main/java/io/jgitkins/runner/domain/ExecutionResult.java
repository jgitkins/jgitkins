package io.jgitkins.runner.domain;

import lombok.Value;

@Value
public class ExecutionResult {
    JobStatus status;
    int exitCode;
}
