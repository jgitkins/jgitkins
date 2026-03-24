package io.jgitkins.server.infrastructure.adapter.config;

import io.jgitkins.server.application.port.out.RuntimeConfigPort;
import io.jgitkins.server.infrastructure.config.RunnerRuntimeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RunnerRuntimeConfigAdapter implements RuntimeConfigPort {

    private final RunnerRuntimeProperties properties;

    @Override
    public String serviceHost() {
        return properties.getServiceHost();
    }

    @Override
    public String restScheme() {
        return properties.getRestScheme();
    }

    @Override
    public Integer restPort() {
        return properties.getRestPort();
    }

    @Override
    public String restBasePath() {
        return properties.getRestBasePath();
    }

    @Override
    public Integer grpcPort() {
        return properties.getGrpcPort();
    }

    @Override
    public Long pollIntervalMs() {
        return properties.getPollIntervalMs();
    }

    @Override
    public Long busyWaitIntervalMs() {
        return properties.getBusyWaitIntervalMs();
    }
}
