package io.jgitkins.server.execution.application.port.out;

import io.jgitkins.server.execution.application.contract.internal.DispatchableJob;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import java.util.Optional;

public interface JobDispatchQueryPort {
    Optional<DispatchableJob> findNextDispatchableJob(RunnerDispatchContext context);
}
