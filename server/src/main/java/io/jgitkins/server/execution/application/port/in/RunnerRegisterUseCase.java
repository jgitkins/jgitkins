package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.application.dto.command.RunnerRegisterCommand;
import io.jgitkins.server.application.dto.result.RunnerRegistrationResult;

public interface RunnerRegisterUseCase {
    RunnerRegistrationResult register(RunnerRegisterCommand command);
}
