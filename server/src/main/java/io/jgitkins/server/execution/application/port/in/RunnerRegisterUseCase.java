package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.command.RunnerRegisterCommand;
import io.jgitkins.server.execution.application.contract.result.RunnerRegistrationResult;

public interface RunnerRegisterUseCase {
    RunnerRegistrationResult register(RunnerRegisterCommand command);
}
