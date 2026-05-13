package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.application.dto.command.JobCreateCommand;

public interface JobCreateUseCase {
    void create(JobCreateCommand command);
}
