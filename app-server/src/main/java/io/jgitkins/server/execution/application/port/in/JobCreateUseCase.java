package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.JobCreateCommand;

public interface JobCreateUseCase {
    void create(JobCreateCommand command);
}
