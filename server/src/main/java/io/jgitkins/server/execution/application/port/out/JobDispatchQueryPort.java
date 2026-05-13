package io.jgitkins.server.execution.application.port.out;

import io.jgitkins.server.application.dto.DispatchableJob;
import io.jgitkins.server.application.dto.RunnerDispatchContext;
import java.util.Optional;

public interface JobDispatchQueryPort {
    Optional<DispatchableJob> findNextDispatchableJob(RunnerDispatchContext context);
}
