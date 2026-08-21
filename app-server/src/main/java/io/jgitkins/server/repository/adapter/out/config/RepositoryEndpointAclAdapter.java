package io.jgitkins.server.repository.adapter.out.config;

import io.jgitkins.server.execution.application.port.out.RuntimeConfigPort;
import io.jgitkins.server.repository.application.port.out.RepositoryEndpointPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryEndpointAclAdapter implements RepositoryEndpointPort {
    private final RuntimeConfigPort runtimeConfigPort;

    @Override
    public String restScheme() {
        return runtimeConfigPort.restScheme();
    }

    @Override
    public String serviceHost() {
        return runtimeConfigPort.serviceHost();
    }
}
