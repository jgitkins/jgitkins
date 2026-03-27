package io.jgitkins.runner.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RunnerRuntimeConfigResult {

    private final String restHost;
    private final Integer restPort;
    private final String restBasePath;

    private final String grpcHost;
    private final Integer grpcPort;

    private final Long pollIntervalMs;
    private final Long busyWaitIntervalMs;
}
