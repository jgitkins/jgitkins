package io.jgitkins.runner.application.port.in;

import io.jgitkins.runner.domain.ExecutionResult;

public interface JobExecuteUseCase {
    ExecutionResult execute(JobExecuteCommand command);
}
