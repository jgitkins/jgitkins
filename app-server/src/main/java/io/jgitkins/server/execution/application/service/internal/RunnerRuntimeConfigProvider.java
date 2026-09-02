package io.jgitkins.server.execution.application.service.internal;

import io.jgitkins.server.execution.application.contract.internal.RunnerRuntimeConfig;
import io.jgitkins.server.execution.application.port.out.RuntimeConfigPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RunnerRuntimeConfigProvider {

    private final RuntimeConfigPort runtimeConfigPort;

    public RunnerRuntimeConfig createConfig() {
        return RunnerRuntimeConfig.builder()
                .serviceHost(runtimeConfigPort.serviceHost())
                .restScheme(runtimeConfigPort.restScheme())
                .restPort(runtimeConfigPort.restPort())
                .restBasePath(runtimeConfigPort.restBasePath())
                .grpcPort(runtimeConfigPort.grpcPort())
                .pollIntervalMs(runtimeConfigPort.pollIntervalMs())
                .busyWaitIntervalMs(runtimeConfigPort.busyWaitIntervalMs())
                .build();
    }
}
