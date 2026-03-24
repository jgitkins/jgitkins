package io.jgitkins.server.application.support;

import io.jgitkins.server.application.port.out.RuntimeConfigPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CloneUrlBuilder {

    private final RuntimeConfigPort runtimeConfigPort;

    public String build(String clonePath) {
        if (clonePath == null || clonePath.isBlank()) {
            return null;
        }

        String normalizedPath = clonePath.startsWith("/") ? clonePath : "/" + clonePath;
        return "%s://%s%s".formatted(
                runtimeConfigPort.restScheme(),
                runtimeConfigPort.serviceHost(),
                normalizedPath
        );
    }
}
