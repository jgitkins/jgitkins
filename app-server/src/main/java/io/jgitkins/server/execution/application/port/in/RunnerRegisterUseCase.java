package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.RunnerRegisterCommand;
import io.jgitkins.server.execution.application.contract.RunnerRegistrationResult;

public interface RunnerRegisterUseCase {
    RunnerRegistrationResult register(RunnerRegisterCommand command);
}
