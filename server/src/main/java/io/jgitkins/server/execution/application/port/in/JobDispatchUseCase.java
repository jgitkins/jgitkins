package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.command.DispatchJobCommand;
import io.jgitkins.server.execution.application.contract.result.JobDispatchResult;
import java.util.Optional;

public interface JobDispatchUseCase {
    Optional<JobDispatchResult> dispatch(DispatchJobCommand command);
}
