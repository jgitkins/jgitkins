package io.jgitkins.server.application.dto.command;

import io.jgitkins.server.application.dto.JobResultStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobResultReportCommand {
    private final String runnerToken;
    private final Long jobId;
    private final JobResultStatus status;
}
