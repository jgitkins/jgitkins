package io.jgitkins.runner.application.port.out;

import io.jgitkins.runner.domain.RunnerConfiguration;
import io.jgitkins.server.grpc.JobPayload;
import java.util.Optional;

public interface JobFetchPort {

    Optional<JobPayload> fetchJob(RunnerConfiguration configuration);
}
