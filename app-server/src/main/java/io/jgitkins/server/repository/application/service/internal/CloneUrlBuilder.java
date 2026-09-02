package io.jgitkins.server.repository.application.service.internal;

import io.jgitkins.server.repository.application.port.out.RepositoryEndpointPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CloneUrlBuilder {

    private final RepositoryEndpointPort repositoryEndpointPort;

    public String build(String clonePath) {
        if (clonePath == null || clonePath.isBlank()) {
            return null;
        }

        String normalizedPath = clonePath.startsWith("/") ? clonePath : "/" + clonePath;
        return "%s://%s%s".formatted(
                repositoryEndpointPort.restScheme(),
                repositoryEndpointPort.serviceHost(),
                normalizedPath
        );
    }
}
