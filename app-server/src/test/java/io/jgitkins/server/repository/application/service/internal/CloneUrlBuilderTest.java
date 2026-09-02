package io.jgitkins.server.repository.application.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.port.out.RepositoryEndpointPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloneUrlBuilderTest {
    @Mock RepositoryEndpointPort endpointPort;

    @Test
    void buildNormalizesPathAndDelegatesEndpointConfiguration() {
        when(endpointPort.restScheme()).thenReturn("https");
        when(endpointPort.serviceHost()).thenReturn("git.example.test");

        assertEquals("https://git.example.test/repos/a.git",
                new CloneUrlBuilder(endpointPort).build("repos/a.git"));
    }
}
