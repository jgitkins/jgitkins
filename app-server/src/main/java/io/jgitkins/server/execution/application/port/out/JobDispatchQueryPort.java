package io.jgitkins.server.execution.application.port.out;

import io.jgitkins.server.execution.application.contract.external.DispatchableJob;
import io.jgitkins.server.execution.application.contract.external.RunnerDispatchContext;
import java.util.Optional;

public interface JobDispatchQueryPort {
    Optional<DispatchableJob> fetchNextJob(RunnerDispatchContext context);
}
