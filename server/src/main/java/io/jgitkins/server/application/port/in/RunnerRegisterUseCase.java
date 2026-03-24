package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.RunnerRegisterCommand;
import io.jgitkins.server.application.dto.result.RunnerRegistrationResult;

public interface RunnerRegisterUseCase {
    RunnerRegistrationResult register(RunnerRegisterCommand command);
}
