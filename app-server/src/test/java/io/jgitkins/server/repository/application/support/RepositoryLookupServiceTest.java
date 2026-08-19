package io.jgitkins.server.repository.application.support;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.port.out.OrganizationNamespacePort;
import io.jgitkins.server.repository.application.port.out.UserNamespacePort;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryLookupServiceTest {
    @Mock private RepositoryRepository repositoryRepository;
    @Mock private UserNamespacePort userNamespacePort;
    @Mock private OrganizationNamespacePort organizationNamespacePort;

    @Test
    void resolveByOwner_userNamespace_usesScalarUserIdLookup() {
        when(userNamespacePort.findUserIdByUsername("alice")).thenReturn(Optional.empty());
        RepositoryLookupService service = new RepositoryLookupService(repositoryRepository, userNamespacePort, organizationNamespacePort);
        assertTrue(service.resolveByOwner(io.jgitkins.server.shared.domain.model.vo.OwnerType.USER, "alice", "repo").isEmpty());
        verify(userNamespacePort).findUserIdByUsername("alice");
    }
}
