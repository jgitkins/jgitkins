package io.jgitkins.server.execution.application.contract.command;

import io.jgitkins.server.execution.application.contract.result.JobResultStatus;

public record JobResultReportCommand(
        String runnerToken,
        Long jobId,
        JobResultStatus status
) {
}
