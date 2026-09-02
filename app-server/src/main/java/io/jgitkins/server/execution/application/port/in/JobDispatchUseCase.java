package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.DispatchJobCommand;
import io.jgitkins.server.execution.application.contract.JobDispatchResult;
import java.util.Optional;

public interface JobDispatchUseCase {
    Optional<JobDispatchResult> dispatch(DispatchJobCommand command);
}
