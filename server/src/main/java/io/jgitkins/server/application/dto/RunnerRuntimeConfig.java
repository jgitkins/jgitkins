package io.jgitkins.server.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RunnerRuntimeConfig {

    private final String serviceHost;
    private final String restScheme;
    private final Integer restPort;
    private final String restBasePath;
    private final Integer grpcPort;
    private final Long pollIntervalMs;
    private final Long busyWaitIntervalMs;
}
