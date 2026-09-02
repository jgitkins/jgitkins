package io.jgitkins.server.execution.application.contract;

import io.jgitkins.server.execution.application.contract.internal.JobResultStatus;

public record JobResultReportCommand(
        String runnerToken,
        Long jobId,
        JobResultStatus status
) {
}
