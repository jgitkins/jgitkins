package io.jgitkins.server.application.dto.command;

import io.jgitkins.server.application.dto.JobResultStatus;

public record JobResultReportCommand(
        String runnerToken,
        Long jobId,
        JobResultStatus status
) {
}
