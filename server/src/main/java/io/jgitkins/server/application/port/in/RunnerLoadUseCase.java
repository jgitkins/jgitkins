package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.RunnerDetailResult;
import java.util.List;

public interface RunnerLoadUseCase {
    RunnerDetailResult getRunner(Long runnerId);
    List<RunnerDetailResult> getRunners();
}
