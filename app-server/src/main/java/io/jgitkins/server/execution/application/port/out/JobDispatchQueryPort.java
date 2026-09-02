package io.jgitkins.server.execution.application.port.out;

import io.jgitkins.server.execution.application.internal.DispatchableJob;
import io.jgitkins.server.execution.application.internal.RunnerDispatchContext;
import java.util.Optional;

public interface JobDispatchQueryPort {
    Optional<DispatchableJob> fetchNextJob(RunnerDispatchContext context);
}
