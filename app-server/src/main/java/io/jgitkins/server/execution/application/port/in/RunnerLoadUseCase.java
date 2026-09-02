package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.RunnerDetailResult;
import java.util.List;

public interface RunnerLoadUseCase {
    RunnerDetailResult getRunner(Long runnerId);
    List<RunnerDetailResult> getRunners();
}
