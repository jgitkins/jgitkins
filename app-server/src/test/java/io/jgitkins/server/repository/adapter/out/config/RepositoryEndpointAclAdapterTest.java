package io.jgitkins.server.repository.adapter.out.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.jgitkins.server.execution.application.port.out.RuntimeConfigPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryEndpointAclAdapterTest {
    @Mock RuntimeConfigPort runtimeConfigPort;

    @Test
    void delegatesOnlyEndpointValuesToRuntimeConfigPort() {
        when(runtimeConfigPort.restScheme()).thenReturn("https");
        when(runtimeConfigPort.serviceHost()).thenReturn("git.example.test");
        RepositoryEndpointAclAdapter adapter = new RepositoryEndpointAclAdapter(runtimeConfigPort);

        assertEquals("https", adapter.restScheme());
        assertEquals("git.example.test", adapter.serviceHost());
    }
}
