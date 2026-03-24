package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.DispatchJobCommand;
import io.jgitkins.server.application.dto.result.JobDispatchResult;
import java.util.Optional;

public interface JobDispatchUseCase {
    Optional<JobDispatchResult> dispatch(DispatchJobCommand command);
}
