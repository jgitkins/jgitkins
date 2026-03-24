package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.JobCreateCommand;

public interface JobCreateUseCase {
    void create(JobCreateCommand command);
}
