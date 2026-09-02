package io.jgitkins.runner.application.contract;

public record RunnerRuntimeConfigResult(
        String restHost,
        Integer restPort,
        String restBasePath,
        String grpcHost,
        Integer grpcPort,
        Long pollIntervalMs,
        Long busyWaitIntervalMs
) {
}
