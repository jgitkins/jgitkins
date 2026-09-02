package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.JobResultReportCommand;

public interface JobResultReportUseCase {
    void reportJobResult(JobResultReportCommand command);
}
